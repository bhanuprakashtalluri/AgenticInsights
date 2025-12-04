import React, { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import { Bar, Line, Pie } from 'react-chartjs-2';
import { Chart, CategoryScale, LinearScale, BarElement, PointElement, LineElement, ArcElement, Tooltip, Legend } from 'chart.js';
import axios from 'axios';

Chart.register(CategoryScale, LinearScale, BarElement, PointElement, LineElement, ArcElement, Tooltip, Legend);

const SIDEBAR_WIDTH = 180;

const MetricsPage: React.FC = () => {
  const [barData, setBarData] = useState<any>(null);
  const [lineData, setLineData] = useState<any>(null);
  const [pieData, setPieData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchRecognitions = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await axios.get('/recognitions?page=0&size=100');
        const recognitions = Array.isArray(res.data) ? res.data : res.data.content || [];
        // Aggregate by month for bar/line
        const monthMap: Record<string, { count: number; points: number }> = {};
        const teamMap: Record<string, number> = {};
        recognitions.forEach((rec: any) => {
          // sentAt is a unix timestamp (seconds or ms)
          let date = new Date((rec.sentAt && typeof rec.sentAt === 'number') ? rec.sentAt * 1000 : Date.now());
          let month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
          monthMap[month] = monthMap[month] || { count: 0, points: 0 };
          monthMap[month].count += 1;
          monthMap[month].points += rec.awardPoints || 0;
          // Pie: by recipient team/role
          let team = rec.recipientRole || 'Unknown';
          teamMap[team] = (teamMap[team] || 0) + 1;
        });
        const months = Object.keys(monthMap).sort();
        setBarData({
          labels: months,
          datasets: [{
            label: 'Recognitions Sent',
            data: months.map(m => monthMap[m].count),
            backgroundColor: '#1976d2',
          }],
        });
        setLineData({
          labels: months,
          datasets: [{
            label: 'Points Awarded',
            data: months.map(m => monthMap[m].points),
            fill: false,
            borderColor: '#388e3c',
            tension: 0.1,
          }],
        });
        const teams = Object.keys(teamMap);
        setPieData({
          labels: teams,
          datasets: [{
            label: 'Recognitions by Team',
            data: teams.map(t => teamMap[t]),
            backgroundColor: ['#1976d2', '#ffa000', '#388e3c', '#8da1bd', '#e57373', '#81c784'],
          }],
        });
      } catch (err: any) {
        setError('Failed to fetch recognitions for metrics.');
      } finally {
        setLoading(false);
      }
    };
    fetchRecognitions();
  }, []);

  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: '#f5f7fa' }}>
      <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, height: '100vh', position: 'relative', zIndex: 2 }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', minHeight: '100vh' }}>
        <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600 }}>Metrics & Graphs</h2>
        {loading && <div style={{ fontSize: '0.9rem', color: '#888' }}>Loading metrics...</div>}
        {error && <div style={{ color: 'red', fontSize: '0.9rem', marginBottom: 8 }}>{error}</div>}
        {!loading && !error && barData && (
          <div style={{ width: '100%', maxWidth: 600 }}>
            <h3 style={{ fontSize: '1rem', marginBottom: 8 }}>Recognitions Sent (Bar Chart)</h3>
            <Bar data={barData} options={{ plugins: { legend: { display: false } } }} />
          </div>
        )}
        {!loading && !error && lineData && (
          <div style={{ width: '100%', maxWidth: 600 }}>
            <h3 style={{ fontSize: '1rem', marginBottom: 8 }}>Points Awarded (Line Chart)</h3>
            <Line data={lineData} />
          </div>
        )}
        {!loading && !error && pieData && (
          <div style={{ width: '100%', maxWidth: 400 }}>
            <h3 style={{ fontSize: '1rem', marginBottom: 8 }}>Recognitions by Team (Pie Chart)</h3>
            <Pie data={pieData} />
          </div>
        )}
      </div>
    </div>
  );
};

export default MetricsPage;
