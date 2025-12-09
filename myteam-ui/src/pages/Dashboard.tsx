import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Sidebar from '../components/Sidebar';
import { ComposedChart, Bar, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from 'recharts';
import { PieChart, Pie, Cell } from 'recharts';
import { useAuth } from '../services/auth';

const SIDEBAR_WIDTH = 180;

const ROLE_COLORS = {
  employee: '#8884d8',
  teamlead: '#82ca9d',
  manager: '#ffc658',
  unknown: '#d0d0d0',
};

const Dashboard: React.FC = () => {
  const { user } = useAuth();
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
      // Support both array and paginated object
      const data = Array.isArray(res.data) ? res.data : (res.data.content || []);
      setRecentRecognitions(data);
    }).catch(() => {
      setRecentRecognitions([]);
    });
    // Fetch all recognitions for filtering
    axios.get('/recognitions?page=0&size=1000').then(res => {
      const data = Array.isArray(res.data) ? res.data : (res.data.content || []);
      console.log('Raw recognitions data:', data);
      // Role-based filtering
      let filteredData = data;
      if (user) {
        if (user.role === 'employee') {
          filteredData = data.filter((rec: any) => rec.senderEmail === user.email || rec.recipientEmail === user.email);
        } else if (user.role === 'teamlead') {
          filteredData = data.filter((rec: any) => rec.teamleadEmail === user.email);
        } else if (user.role === 'manager') {
          filteredData = data.filter((rec: any) => rec.managerEmail === user.email);
        }
        // Admin sees all
      }
      console.log('Filtered recognitions data:', filteredData);
      setAllRecognitions(filteredData);
      // Aggregate by month for graph
      const monthMap: { [key: string]: { recognitions: number; points: number } } = {};
      filteredData.forEach((rec: any) => {
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
      filteredData.forEach((rec: any) => {
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
  }, [user]);

  // Filtered pie data
  const filteredPieData = selectedRole ? pieData.filter(d => d.role === selectedRole) : pieData;

  return (
    <div style={{ display: 'flex' }}>
      <div style={{ width: SIDEBAR_WIDTH }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: '20px' }}>
        <h1>Dashboard</h1>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
          <div>
            <h2>Metrics</h2>
            <div style={{ display: 'flex', gap: '24px', marginBottom: '24px' }}>
              <div style={{ background: '#fff', borderRadius: '8px', boxShadow: '0 2px 8px #e0e0e0', padding: '18px', minWidth: '160px', textAlign: 'center' }}>
                <div style={{ fontSize: '0.8rem', color: '#888' }}>Total Recognitions</div>
                <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>{metrics.totalRecognitions}</div>
              </div>
              <div style={{ background: '#fff', borderRadius: '8px', boxShadow: '0 2px 8px #e0e0e0', padding: '18px', minWidth: '160px', textAlign: 'center' }}>
                <div style={{ fontSize: '0.8rem', color: '#888' }}>Total Employees</div>
                <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>{metrics.totalEmployees}</div>
              </div>
              <div style={{ background: '#fff', borderRadius: '8px', boxShadow: '0 2px 8px #e0e0e0', padding: '18px', minWidth: '160px', textAlign: 'center' }}>
                <div style={{ fontSize: '0.8rem', color: '#888' }}>Top Sender</div>
                <div style={{ fontSize: '1rem', fontWeight: 600 }}>{metrics.topSender !== '-' ? metrics.topSender : 'No data'}</div>
              </div>
              <div style={{ background: '#fff', borderRadius: '8px', boxShadow: '0 2px 8px #e0e0e0', padding: '18px', minWidth: '160px', textAlign: 'center' }}>
                <div style={{ fontSize: '0.8rem', color: '#888' }}>Top Receiver</div>
                <div style={{ fontSize: '1rem', fontWeight: 600 }}>{metrics.topReceiver !== '-' ? metrics.topReceiver : 'No data'}</div>
              </div>
            </div>
            <h2>Recent Activity</h2>
            <table style={{ width: '100%', fontSize: '0.8rem', background: '#fff', borderRadius: '8px' }}>
              <thead>
                <tr style={{ background: '#8da1bd' }}>
                  <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Sender</th>
                  <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Recipient</th>
                  <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Message</th>
                  <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Points</th>
                  <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Date</th>
                </tr>
              </thead>
              <tbody>
                {recentRecognitions.length === 0 ? (
                  <tr><td colSpan={5} style={{ padding: 8, textAlign: 'center', color: '#888' }}>No recent recognitions found.</td></tr>
                ) : recentRecognitions.map((row, idx) => {
                  const sender = row.senderName || '-';
                  const receiver = row.recipientName || '-';
                  const message = row.message || '-';
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
                      <td style={{ padding: 8 }}>{message}</td>
                      <td style={{ padding: 8 }}>{points}</td>
                      <td style={{ padding: 8 }}>{dateStr}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <h2>Graph - Recognitions and Points Over Time</h2>
            {graphData.length === 0 ? (
              <div style={{ color: '#888', textAlign: 'center', margin: '24px 0' }}>No data for graph.</div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <ComposedChart data={graphData}>
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <CartesianGrid strokeDasharray="3 3" />
                  <Bar dataKey="recognitions" stackId="a" fill="#8884d8" />
                  <Bar dataKey="points" stackId="a" fill="#82ca9d" />
                </ComposedChart>
              </ResponsiveContainer>
            )}
            <h2>Pie Chart - Recognitions by Role</h2>
            {filteredPieData.length === 0 ? (
              <div style={{ color: '#888', textAlign: 'center', margin: '24px 0' }}>No data for pie chart.</div>
            ) : (
              <PieChart width={400} height={400}>
                <Pie
                  data={filteredPieData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  fill="#8884d8"
                  label
                >
                  {filteredPieData.map((entry, index) => {
                    const roleKey: keyof typeof ROLE_COLORS = typeof entry.role === 'string' && ROLE_COLORS.hasOwnProperty(entry.role)
                      ? entry.role as keyof typeof ROLE_COLORS
                      : 'unknown';
                    return (
                      <Cell key={`cell-${index}`} fill={ROLE_COLORS[roleKey]} />
                    );
                  })}
                </Pie>
                <Tooltip />
              </PieChart>
            )}
            <h2>Debug: Raw Recognitions Data</h2>
            <table style={{ width: '100%', fontSize: '0.8rem', background: '#fff', borderRadius: '8px', marginBottom: '24px' }}>
              <thead>
                <tr style={{ background: '#e0e0e0' }}>
                  {Object.keys(recentRecognitions[0] || {}).map((key) => (
                    <th key={key} style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>{key}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {recentRecognitions.map((row, idx) => (
                  <tr key={idx} style={{ background: idx % 2 === 0 ? '#f5f7fa' : '#fff' }}>
                    {Object.keys(row).map((key) => (
                      <td key={key} style={{ padding: 8 }}>{String(row[key])}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
