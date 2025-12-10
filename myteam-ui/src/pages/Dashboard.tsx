import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Sidebar from '../components/Sidebar';
import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell } from 'recharts';
import { useAuth } from '../services/auth';

const SIDEBAR_WIDTH = 180;

const ROLE_COLORS = {
  employee: '#8884d8',
  teamlead: '#82ca9d',
  manager: '#ffc658',
  unknown: '#d0d0d0',
};

const Dashboard: React.FC<{ context?: 'self' | 'team' | 'unit' }> = ({ context }) => {
  const { user } = useAuth();
  // Metrics
  const [metrics, setMetrics] = useState({
    totalRecognitions: 0,
    totalEmployees: 0,
    topSender: '-',
    topReceiver: '-',
  });
  // Recent activity (not used anymore after scoped table)
  // const [recentRecognitions, setRecentRecognitions] = useState<any[]>([]);
  // Full scoped recognitions for table
  const [scopedRecognitions, setScopedRecognitions] = useState<any[]>([]);
  // Graph data for last 12 months
  const [graphData, setGraphData] = useState<any[]>([]);
  // Pie chart data for recognitions by role
  const [pieData, setPieData] = useState<any[]>([]);
  // Filters for linking
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<string | null>(null);
  // Table controls
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [search, setSearch] = useState('');
  const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

  // Employees-driven scoping
  const [scopeNames, setScopeNames] = useState<string[]>([]);

  const norm = (v: any) => (typeof v === 'string' ? v.trim().toLowerCase() : v ?? '');

  // Derive scope names based on user and context
  useEffect(() => {
    if (!user) return;
    // Fetch employees to derive team/unit membership
    axios.get('/employees?page=0&size=5000').then(res => {
      const list: any[] = Array.isArray(res.data) ? res.data : (res.data.content || []);
      const names = new Set<string>();
      const fullName = (e: any) => [e.firstName, e.lastName].filter(Boolean).join(' ').trim();

      if (context === 'self' || user.role === 'employee') {
        // Self scope: only the user's own name
        const selfEmp = list.find((e: any) => norm(e.email) === norm(user.email));
        if (selfEmp) names.add(fullName(selfEmp));
      } else if (context === 'team' || user.role === 'teamlead') {
        // Team scope: members where managerId matches this team lead's id
        // Try to find the TL's employee record to get managerId reference
        const tl = list.find((e: any) => norm(e.email) === norm(user.email));
        const managerId = tl?.id ?? tl?.employeeId ?? tl?.managerId ?? null;
        list.filter((e: any) => e.managerId === managerId).forEach((e: any) => names.add(fullName(e)));
        // Include TL themselves
        if (tl) names.add(fullName(tl));
      } else if (context === 'unit' || user.role === 'manager') {
        // Unit scope: all employees in the manager's unitId
        const mgr = list.find((e: any) => norm(e.email) === norm(user.email));
        const unitId = mgr?.unitId ?? user.unitId;
        list.filter((e: any) => String(e.unitId) === String(unitId)).forEach((e: any) => names.add(fullName(e)));
        if (mgr) names.add(fullName(mgr));
      }
      setScopeNames(Array.from(names));
    }).catch(() => {
      setScopeNames([]);
    });
  }, [user, context]);

  // Fetch summary metrics not scoped by employees (they are global counts)
  useEffect(() => {
    axios.get('/metrics/summary').then(res => {
      const totals = res.data.totals || {};
      setMetrics(prev => ({
        ...prev,
        totalRecognitions: totals.count || 0,
      }));
    }).catch(() => {
      setMetrics(prev => ({ ...prev, totalRecognitions: 0 }));
    });
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
      setMetrics(prev => ({ ...prev, topSender: name !== '-' ? name : 'No data' }));
    }).catch(() => {
      setMetrics(prev => ({ ...prev, topSender: 'No data' }));
    });
    axios.get('/leaderboard/top-recipients?page=0&size=1').then(res => {
      let name = '-';
      if (res.data && Array.isArray(res.data.content) && res.data.content.length > 0) {
        name = res.data.content[0].name || '-';
      }
      setMetrics(prev => ({ ...prev, topReceiver: name !== '-' ? name : 'No data' }));
    }).catch(() => {
      setMetrics(prev => ({ ...prev, topReceiver: 'No data' }));
    });
  }, []);

  // Fetch recognitions and apply strict employees-driven scoping
  useEffect(() => {
    if (!user) return;
    axios.get('/recognitions?page=0&size=1000').then(res => {
      const data = Array.isArray(res.data) ? res.data : (res.data.content || []);
      // Build a set for quick lookup
      const nameSet = new Set(scopeNames);
      const filtered = data.filter((rec: any) => {
        const s = rec.senderName || '';
        const r = rec.recipientName || '';
        return nameSet.has(s) || nameSet.has(r);
      });
      // Store full scoped recognitions
      setScopedRecognitions(filtered);
      // Aggregate by month for graph (last 12 months only)
      const monthMap: { [key: string]: { recognitions: number; points: number; roles: Record<string, number> } } = {};
      filtered.forEach((rec: any) => {
        if (!rec.sentAt) return;
        const date = new Date(rec.sentAt * 1000);
        const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        if (!monthMap[key]) monthMap[key] = { recognitions: 0, points: 0, roles: {} };
        monthMap[key].recognitions += 1;
        monthMap[key].points += rec.awardPoints || 0;
        const role = rec.recipientRole || 'unknown';
        monthMap[key].roles[role] = (monthMap[key].roles[role] || 0) + 1;
      });
      const sortedKeys = Object.keys(monthMap).sort((a, b) => a.localeCompare(b));
      const last12 = sortedKeys.slice(-12);
      const chartData = last12.map(key => ({
        month: key,
        recognitions: monthMap[key].recognitions,
        points: monthMap[key].points,
        roles: monthMap[key].roles
      }));
      setGraphData(chartData);
      // Pie chart: aggregate by role for last 12 months only
      const pieRoleMap: { [key: string]: number } = {};
      chartData.forEach(month => {
        Object.entries(month.roles).forEach(([role, count]) => {
          pieRoleMap[role] = (pieRoleMap[role] || 0) + (count as number);
        });
      });
      const pieChartData = Object.keys(pieRoleMap).map(role => ({
        name: role.charAt(0).toUpperCase() + role.slice(1),
        value: pieRoleMap[role],
        role,
      }));
      setPieData(pieChartData);
    }).catch(() => {
      setScopedRecognitions([]);
      setGraphData([]);
      setPieData([]);
    });
  }, [user, scopeNames]);

  // Derive table rows based on search and chart filters
  const tableFiltered = (() => {
    let rows = scopedRecognitions;
    // Apply graph selectedRole filter by recipientRole
    if (selectedRole) {
      rows = rows.filter(r => (r.recipientRole || 'unknown') === selectedRole);
    }
    // Apply selectedMonth filter by sentAt month
    if (selectedMonth) {
      rows = rows.filter(r => {
        if (!r.sentAt) return false;
        const d = new Date(r.sentAt * 1000);
        const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        return key === selectedMonth;
      });
    }
    // Apply search across key fields
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      rows = rows.filter(rec =>
        [rec.id, rec.recognitionTypeName, rec.category, rec.level, rec.awardPoints, rec.senderName, rec.recipientName, rec.message, rec.approvalStatus]
          .map(val => (val !== undefined && val !== null ? String(val).toLowerCase() : ''))
          .some(val => val.includes(q))
      );
    }
    return rows;
  })();

  const pagedRows = (() => {
    const total = Math.max(1, Math.ceil(tableFiltered.length / pageSize));
    if (totalPages !== total) setTotalPages(total);
    const start = page * pageSize;
    return tableFiltered.slice(start, start + pageSize);
  })();

  const handlePageChange = (newPage: number) => setPage(newPage);
  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => { setPage(0); setPageSize(parseInt(e.target.value, 10)); };

  // Pie chart role filtering logic
  let filteredPieData = pieData;
  let showPieChart = true;
  if (user) {
    if (user.role === 'employee') {
      showPieChart = false;
    } else if (user.role === 'teamlead') {
      filteredPieData = pieData.filter(d => d.role === 'employee' || d.role === 'teamlead');
    }
  }

  // Only filter by selectedRole for both graphs and pie, and by selectedMonth for pie
  let filteredGraphData = graphData;
  if (selectedRole) {
    filteredGraphData = graphData.map(d => ({
      ...d,
      recognitions: d.roles[selectedRole] || 0,
      points: d.roles[selectedRole] || 0
    }));
  }

  // Pie chart: filter by selectedMonth and selectedRole
  let pieMonthData = filteredPieData;
  if (selectedMonth) {
    const monthObj = graphData.find(d => d.month === selectedMonth);
    if (monthObj) {
      pieMonthData = Object.keys(monthObj.roles).map(role => ({
        name: role.charAt(0).toUpperCase() + role.slice(1),
        value: monthObj.roles[role],
        role,
      }));
      if (user && user.role === 'teamlead') {
        pieMonthData = pieMonthData.filter(d => d.role === 'employee' || d.role === 'teamlead');
      }
      if (selectedRole) {
        pieMonthData = pieMonthData.filter(d => d.role === selectedRole);
      }
    } else {
      pieMonthData = [];
    }
  } else if (selectedRole) {
    pieMonthData = pieMonthData.filter(d => d.role === selectedRole);
  }

  const handlePieClick = (data: any) => {
    setSelectedRole(selectedRole === data.role ? null : data.role);
  };

  const handleBarClick = (data: any) => {
    setSelectedMonth(selectedMonth === data.month ? null : data.month);
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f5f7fa' }}>
      <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, background: '#fff', boxShadow: '2px 0 8px #e0e0e0' }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: '20px', marginLeft: 0 }}>
        <h1>Dashboard</h1>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '24px', margin: '24px 0 32px 0' }}>
          <div style={{ display: 'flex', gap: '24px' }}>
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
        </div>
        <div style={{ display: 'flex', flexDirection: 'row', gap: '32px', marginBottom: '32px' }}>
          <div style={{ width: 400 }}>
            <h2>Recognitions Over Time</h2>
            {filteredGraphData.length === 0 ? (
              <div style={{ color: '#888', textAlign: 'center', margin: '24px 0' }}>No data for recognitions graph.</div>
            ) : (
              <ResponsiveContainer width={400} height={220}>
                <BarChart data={filteredGraphData}>
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <CartesianGrid strokeDasharray="3 3" />
                  <Bar dataKey="recognitions" fill="#8884d8" name="Recognitions" onClick={handleBarClick} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
          <div style={{ width: 400 }}>
            <h2>Points Over Time</h2>
            {filteredGraphData.length === 0 ? (
              <div style={{ color: '#888', textAlign: 'center', margin: '24px 0' }}>No data for points graph.</div>
            ) : (
              <ResponsiveContainer width={400} height={220}>
                <BarChart data={filteredGraphData}>
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <CartesianGrid strokeDasharray="3 3" />
                  <Bar dataKey="points" fill="#82ca9d" name="Points" onClick={handleBarClick} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
          {showPieChart && (
            <div style={{ width: 320 }}>
              <h2>Recognitions by Role</h2>
              {pieMonthData.length === 0 ? (
                <div style={{ color: '#888', textAlign: 'center', margin: '24px 0' }}>No data for pie chart.</div>
              ) : (
                <>
                  <PieChart width={320} height={320}>
                    <Pie
                      data={pieMonthData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      outerRadius={60}
                      fill="#8884d8"
                      label
                      onClick={handlePieClick}
                    >
                      {pieMonthData.map((entry, index) => {
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
                  <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', marginTop: 12 }}>
                    {pieMonthData.map((entry) => (
                      <div key={entry.role} style={{ display: 'flex', alignItems: 'center', margin: '0 8px 4px 0', fontSize: 13 }}>
                        <span style={{
                          display: 'inline-block',
                          width: 14,
                          height: 14,
                          background: ROLE_COLORS[entry.role as keyof typeof ROLE_COLORS] || '#ccc',
                          borderRadius: 3,
                          marginRight: 6
                        }}></span>
                        {entry.name}
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}
        </div>
        {/* Scoped Recognitions Table (linked with graph/pie filters) */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8, marginBottom: 10 }}>
          <h2 style={{ margin: 0 }}>Recognitions</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <input
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search recognitions..."
              style={{ padding: '7px 12px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 240 }}
            />
            <label style={{ fontSize: '0.8rem' }}>Page Size:</label>
            <select value={pageSize} onChange={handlePageSizeChange} style={{ padding: 5, fontSize: '0.8rem' }}>
              {PAGE_SIZE_OPTIONS.map(size => (
                <option key={size} value={size}>{size}</option>
              ))}
            </select>
            <span style={{ marginLeft: 12, fontSize: '0.8rem' }}>Page <b>{page + 1}</b> / <b>{totalPages}</b></span>
            <div style={{ display: 'flex', gap: 8 }}>
              <button onClick={() => handlePageChange(page - 1)} disabled={page <= 0} style={{ padding: '6px 12px', fontSize: '0.8rem', background: '#eee', border: '1px solid #ccc', borderRadius: 6, cursor: page <= 0 ? 'not-allowed' : 'pointer' }}>Prev</button>
              <button onClick={() => handlePageChange(page + 1)} disabled={page + 1 >= totalPages} style={{ padding: '6px 12px', fontSize: '0.8rem', background: '#eee', border: '1px solid #ccc', borderRadius: 6, cursor: page + 1 >= totalPages ? 'not-allowed' : 'pointer' }}>Next</button>
            </div>
          </div>
        </div>
        <table style={{ width: '100%', fontSize: '0.8rem', background: '#fff', borderRadius: '8px', marginBottom: '24px', tableLayout: 'fixed' }}>
          <colgroup>
            <col style={{ width: '6%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '10%' }} />
          </colgroup>
          <thead>
            <tr style={{ background: '#8da1bd' }}>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>ID</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Type</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Category</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Level</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Points</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Sender</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Recipient</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Message</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Date</th>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Approval</th>
            </tr>
          </thead>
          <tbody>
            {pagedRows.length === 0 ? (
              <tr><td colSpan={10} style={{ padding: 8, textAlign: 'center', color: '#888' }}>No recognitions found.</td></tr>
            ) : pagedRows.map((row, idx) => {
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
                  <td style={{ padding: 8 }}>{row.id}</td>
                  <td style={{ padding: 8 }}>{row.recognitionTypeName || '-'}</td>
                  <td style={{ padding: 8 }}>{row.category || '-'}</td>
                  <td style={{ padding: 8 }}>{row.level || '-'}</td>
                  <td style={{ padding: 8 }}>{points}</td>
                  <td style={{ padding: 8 }}>{sender}</td>
                  <td style={{ padding: 8 }}>{receiver}</td>
                  <td style={{ padding: 8, overflow: 'hidden', textOverflow: 'ellipsis' }}>{message}</td>
                  <td style={{ padding: 8 }}>{dateStr}</td>
                  <td style={{ padding: 8 }}>{row.approvalStatus || '-'}</td>
                 </tr>
               );
             })}
           </tbody>
         </table>
      </div>
    </div>
  );
};

export default Dashboard;
