import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Sidebar from '../components/Sidebar';
import { ComposedChart, Bar, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from 'recharts';
import { PieChart, Pie, Cell } from 'recharts';

const SIDEBAR_WIDTH = 180;

const ROLE_COLORS = {
  employee: '#8884d8',
  teamlead: '#82ca9d',
  manager: '#ffc658',
  unknown: '#d0d0d0',
};

const Dashboard: React.FC = () => {
  // Metrics
  const [metrics, setMetrics] = useState({
    totalRecognitions: 0,
    totalEmployees: 0,
    topSender: '-',
    topReceiver: '-',
  });
  // Recent activity
  const [recentRecognitions, setRecentRecognitions] = useState<any[]>([]);
  // Graph data for last 6 months
  const [graphData, setGraphData] = useState<any[]>([]);
  // Pie chart data for recognitions by role
  const [pieData, setPieData] = useState<any[]>([]);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<string | null>(null);
  // Store all recognitions for filtering
  const [allRecognitions, setAllRecognitions] = useState<any[]>([]);

  useEffect(() => {
    // Fetch total recognitions
    axios.get('/metrics/summary').then(res => {
      const totals = res.data.totals || {};
      setMetrics(prev => ({
        ...prev,
        totalRecognitions: totals.count || 0,
      }));
    }).catch(() => {
      setMetrics(prev => ({ ...prev, totalRecognitions: 0 }));
    });
    // Fetch total employees
    axios.get('/employees?page=0&size=1').then(res => {
      let total = 0;
      if (res.data && typeof res.data.totalElements === 'number') {
        total = res.data.totalElements;
      } else if (Array.isArray(res.data)) {
        total = res.data.length;
      }
      setMetrics(prev => ({ ...prev, totalEmployees: total }));
    }).catch(() => {
      setMetrics(prev => ({ ...prev, totalEmployees: 0 }));
    });
    // Fetch top sender
    axios.get('/leaderboard/top-senders?page=0&size=1').then(res => {
      let name = '-';
      if (res.data) {
        if (Array.isArray(res.data)) {
          if (res.data.length > 0 && res.data[0].name) {
            name = res.data[0].name;
          }
        } else if (Array.isArray(res.data.content)) {
          if (res.data.content.length > 0 && res.data.content[0].name) {
            name = res.data.content[0].name;
          }
        } else if (res.data.name) {
          name = res.data.name;
        }
      }
      console.log('Top Sender API response:', res.data, 'Parsed:', name);
      setMetrics(prev => ({ ...prev, topSender: name !== '-' ? name : 'No data' }));
    }).catch(err => {
      console.log('Top Sender API error:', err);
      setMetrics(prev => ({ ...prev, topSender: 'No data' }));
    });
    // Fetch top receiver (correct endpoint)
    axios.get('/leaderboard/top-recipients?page=0&size=1').then(res => {
      let name = '-';
      if (res.data && Array.isArray(res.data.content) && res.data.content.length > 0) {
        name = res.data.content[0].name || '-';
      }
      console.log('Top Receiver API response:', res.data, 'Parsed:', name);
      setMetrics(prev => ({ ...prev, topReceiver: name !== '-' ? name : 'No data' }));
    }).catch(err => {
      console.log('Top Receiver API error:', err);
      setMetrics(prev => ({ ...prev, topReceiver: 'No data' }));
    });
    axios.get('/recognitions?page=0&size=10').then(res => {
      setRecentRecognitions(res.data);
    }).catch(() => {
      setRecentRecognitions([]);
    });
    // Fetch all recognitions for filtering
    axios.get('/recognitions?page=0&size=1000').then(res => {
      const data = Array.isArray(res.data) ? res.data : (res.data.content || []);
      setAllRecognitions(data);
      // Aggregate by month for graph
      const monthMap: { [key: string]: { recognitions: number; points: number } } = {};
      data.forEach((rec: any) => {
        if (!rec.sentAt) return;
        const date = new Date(rec.sentAt * 1000);
        const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        if (!monthMap[key]) monthMap[key] = { recognitions: 0, points: 0 };
        monthMap[key].recognitions += 1;
        monthMap[key].points += rec.awardPoints || 0;
      });
      const sortedKeys = Object.keys(monthMap).sort((a, b) => b.localeCompare(a));
      const last12 = sortedKeys.slice(0, 12).reverse();
      const chartData = last12.map(key => ({
        month: key,
        recognitions: monthMap[key].recognitions,
        points: monthMap[key].points,
      }));
      setGraphData(chartData);
      // Aggregate by role for pie chart
      const roleMap: { [key: string]: number } = {};
      data.forEach((rec: any) => {
        const role = rec.recipientRole || 'unknown';
        roleMap[role] = (roleMap[role] || 0) + 1;
      });
      const pieChartData = Object.keys(roleMap).map(role => ({
        name: role.charAt(0).toUpperCase() + role.slice(1),
        value: roleMap[role],
        role,
      }));
      setPieData(pieChartData);
    }).catch(() => {
      setAllRecognitions([]);
      setGraphData([]);
      setPieData([]);
    });
  }, []);

  // Filtered pie chart data based on selected month and role
  const filteredPieData = React.useMemo(() => {
    let filtered = allRecognitions;
    if (selectedMonth) {
      filtered = filtered.filter((rec: any) => {
        if (!rec.sentAt) return false;
        const date = new Date(rec.sentAt * 1000);
        const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        return key === selectedMonth;
      });
    }
    if (selectedRole) {
      filtered = filtered.filter((rec: any) => rec.recipientRole === selectedRole);
    }
    // Aggregate by role
    const roleMap: { [key: string]: number } = {};
    filtered.forEach((rec: any) => {
      const role = rec.recipientRole || 'unknown';
      roleMap[role] = (roleMap[role] || 0) + 1;
    });
    return Object.keys(roleMap).map(role => ({
      name: role.charAt(0).toUpperCase() + role.slice(1),
      value: roleMap[role],
      role,
    }));
  }, [allRecognitions, selectedMonth, selectedRole]);

  // Filtered graph data based on selected role
  const filteredGraphData = React.useMemo(() => {
    let filtered = allRecognitions;
    if (selectedRole) {
      filtered = filtered.filter((rec: any) => rec.recipientRole === selectedRole);
    }
    // Aggregate by month
    const monthMap: { [key: string]: { recognitions: number; points: number } } = {};
    filtered.forEach((rec: any) => {
      if (!rec.sentAt) return;
      const date = new Date(rec.sentAt * 1000);
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      if (!monthMap[key]) monthMap[key] = { recognitions: 0, points: 0 };
      monthMap[key].recognitions += 1;
      monthMap[key].points += rec.awardPoints || 0;
    });
    const sortedKeys = Object.keys(monthMap).sort((a, b) => b.localeCompare(a));
    const last12 = sortedKeys.slice(0, 12).reverse();
    return last12.map(key => ({
      month: key,
      recognitions: monthMap[key].recognitions,
      points: monthMap[key].points,
    }));
  }, [selectedRole, allRecognitions]);

  // Track active pie segment for highlight
  const activeIndex = selectedRole
    ? filteredPieData.findIndex(d => d.role === selectedRole)
    : undefined;

  // Pie click handler
  const handlePieClick = (_: any, idx: number) => {
    const role = filteredPieData[idx]?.role;
    if (!role) return;
    setSelectedRole(prev => (prev === role ? null : role));
  };

  // Graph click handler
  const handleGraphClick = (data: any, idx: number) => {
    const month = data && data.activeLabel;
    if (!month) return;
    setSelectedMonth(prev => (prev === month ? null : month));
  };

  // Reset filter buttons
  const handleResetRoleFilter = () => setSelectedRole(null);
  const handleResetMonthFilter = () => setSelectedMonth(null);

  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: '#f5f7fa' }}>
      <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, height: '100vh', position: 'relative', zIndex: 2 }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', minHeight: '100vh' }}>
        <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600 }}>Dashboard</h2>
        {/* Metrics Section - moved above graph */}
        <div style={{ display: 'flex', gap: 24, marginBottom: 24, width: '100%', justifyContent: 'center' }}>
          <div style={{ background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18, minWidth: 160, textAlign: 'center' }}>
            <div style={{ fontSize: '0.8rem', color: '#888' }}>Total Recognitions</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>{metrics.totalRecognitions}</div>
          </div>
          <div style={{ background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18, minWidth: 160, textAlign: 'center' }}>
            <div style={{ fontSize: '0.8rem', color: '#888' }}>Total Employees</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>{metrics.totalEmployees}</div>
          </div>
          <div style={{ background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18, minWidth: 160, textAlign: 'center' }}>
            <div style={{ fontSize: '0.8rem', color: '#888' }}>Top Sender</div>
            <div style={{ fontSize: '1rem', fontWeight: 600 }}>{metrics.topSender !== '-' ? metrics.topSender : 'No data'}</div>
          </div>
          <div style={{ background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18, minWidth: 160, textAlign: 'center' }}>
            <div style={{ fontSize: '0.8rem', color: '#888' }}>Top Receiver</div>
            <div style={{ fontSize: '1rem', fontWeight: 600 }}>{metrics.topReceiver !== '-' ? metrics.topReceiver : 'No data'}</div>
          </div>
        </div>
        {/* Pie Chart Section */}
        <div style={{ width: '100%', maxWidth: 700, background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18, marginBottom: 24 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>Recognitions by Recipient Role</div>
          {(selectedRole || selectedMonth) && (
            <div style={{ marginBottom: 8 }}>
              {selectedRole && (
                <button onClick={handleResetRoleFilter} style={{ marginRight: 8, padding: '4px 12px', borderRadius: 4, border: '1px solid #888', background: '#f5f7fa', cursor: 'pointer' }}>
                  Reset Role Filter
                </button>
              )}
              {selectedMonth && (
                <button onClick={handleResetMonthFilter} style={{ padding: '4px 12px', borderRadius: 4, border: '1px solid #888', background: '#f5f7fa', cursor: 'pointer' }}>
                  Reset Month Filter
                </button>
              )}
            </div>
          )}
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={filteredPieData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={80}
                label={({ name, value }) => `${name}: ${value}`}
                activeIndex={activeIndex}
                onClick={handlePieClick}
              >
                {filteredPieData.map((entry, idx) => {
                  const validRoles = ['employee', 'teamlead', 'manager', 'unknown'] as const;
                  const roleKey = validRoles.includes(entry.role as typeof validRoles[number])
                    ? (entry.role as typeof validRoles[number])
                    : 'unknown';
                  return (
                    <Cell key={`cell-${idx}`} fill={ROLE_COLORS[roleKey]} />
                  );
                })}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
        </div>
        {/* Graph Section */}
        <div style={{ width: '100%', maxWidth: 700, background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18, marginBottom: 24 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>
            Recognitions & Points (Last 6 Months)
            {selectedRole && (
              <span style={{ marginLeft: 12, color: '#888', fontWeight: 400 }}>
                (Filtered by role: <b>{selectedRole.charAt(0).toUpperCase() + selectedRole.slice(1)}</b>)
              </span>
            )}
            {selectedMonth && (
              <span style={{ marginLeft: 12, color: '#888', fontWeight: 400 }}>
                (Filtered by month: <b>{selectedMonth}</b>)
              </span>
            )}
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <ComposedChart data={filteredGraphData} margin={{ top: 20, right: 30, left: 0, bottom: 0 }} onClick={handleGraphClick}>
              <CartesianGrid stroke="#f5f5f5" />
              <XAxis dataKey="month" />
              <YAxis yAxisId="left" orientation="left" stroke="#8884d8" />
              <YAxis yAxisId="right" orientation="right" stroke="#82ca9d" />
              <Tooltip />
              <Legend />
              <Bar yAxisId="left" dataKey="recognitions" name="Recognitions" fill="#8884d8" barSize={32} />
              <Line yAxisId="right" type="monotone" dataKey="points" name="Points" stroke="#82ca9d" strokeWidth={3} dot={{ r: 4 }} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
        {/* Recent Recognitions Table */}
        <div style={{ width: '100%', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>Recent Recognitions</div>
          <table style={{ width: '100%', fontSize: '0.8rem', background: '#fff', borderRadius: 8 }}>
            <thead>
              <tr style={{ background: '#8da1bd' }}>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Sender</th>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Receiver</th>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Type</th>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Category</th>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Level</th>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Points</th>
                <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Date</th>
              </tr>
            </thead>
            <tbody>
              {recentRecognitions.length === 0 ? (
                <tr><td colSpan={7} style={{ padding: 8, textAlign: 'center', color: '#888' }}>No recent recognitions found.</td></tr>
              ) : recentRecognitions.map((row, idx) => {
                const sender = row.senderName || '-';
                const receiver = row.recipientName || '-';
                const type = row.recognitionTypeName || '-';
                const category = row.category || '-';
                const level = row.level || '-';
                const points = row.awardPoints != null ? row.awardPoints : '-';
                let dateStr = '-';
                if (row.sentAt) {
                  try {
                    const date = new Date(row.sentAt * 1000);
                    dateStr = `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
                  } catch {
                    dateStr = '-';
                  }
                }
                return (
                  <tr key={idx} style={{ background: idx % 2 === 0 ? '#f5f7fa' : '#fff' }}>
                    <td style={{ padding: 8 }}>{sender}</td>
                    <td style={{ padding: 8 }}>{receiver}</td>
                    <td style={{ padding: 8 }}>{type}</td>
                    <td style={{ padding: 8 }}>{category}</td>
                    <td style={{ padding: 8 }}>{level}</td>
                    <td style={{ padding: 8 }}>{points}</td>
                    <td style={{ padding: 8 }}>{dateStr}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

