import React, { useState } from 'react';
import { Bar, Line, Pie } from 'react-chartjs-2';
import { Chart, CategoryScale, LinearScale, BarElement, PointElement, LineElement, ArcElement, Tooltip, Legend } from 'chart.js';
import Pagination from '../components/Pagination';
Chart.register(CategoryScale, LinearScale, BarElement, PointElement, LineElement, ArcElement, Tooltip, Legend);

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];

const MetricsManagement: React.FC = () => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);

  // Example data, replace with real API data as needed
  const barData = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
    datasets: [
      {
        label: 'Recognitions',
        data: [12, 19, 3, 5, 2],
        backgroundColor: '#8da1bd',
      },
    ],
  };
  const lineData = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
    datasets: [
      {
        label: 'Points Awarded',
        data: [120, 190, 30, 50, 20],
        borderColor: '#8da1bd',
        backgroundColor: 'rgba(141,161,189,0.2)',
      },
    ],
  };
  const pieData = {
    labels: ['Team A', 'Team B', 'Team C'],
    datasets: [
      {
        label: 'Recognitions by Team',
        data: [300, 50, 100],
        backgroundColor: ['#8da1bd', '#b8c6e0', '#e0e7ef'],
      },
    ],
  };

  const fetchMetrics = (newPage: number, newPageSize: number) => {
    setPage(newPage);
    setPageSize(newPageSize);
    // If you fetch paginated metrics, update totalPages here
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    // If you fetch paginated metrics, call fetchMetrics(newPage, pageSize)
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPageSize(newSize);
    setPage(0);
    // If you fetch paginated metrics, call fetchMetrics(0, newSize)
  };

  return (
    <div style={{ width: '100%', padding: 24 }}>
      <h3 style={{ textAlign: 'center', fontSize: '1.2rem', fontWeight: 600, marginBottom: 24 }}>Metrics & Graphs</h3>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, justifyContent: 'center' }}>
        <div style={{ width: 400, background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 16 }}>
          <h4 style={{ textAlign: 'center', marginBottom: 12 }}>Recognitions (Bar)</h4>
          <Bar data={barData} />
        </div>
        <div style={{ width: 400, background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 16 }}>
          <h4 style={{ textAlign: 'center', marginBottom: 12 }}>Points Awarded (Line)</h4>
          <Line data={lineData} />
        </div>
        <div style={{ width: 400, background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 16 }}>
          <h4 style={{ textAlign: 'center', marginBottom: 12 }}>Recognitions by Team (Pie)</h4>
          <Pie data={pieData} />
        </div>
      </div>
      <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 14, fontSize: '0.7rem', justifyContent: 'center', width: '100%' }}>
        <label>Page Size:</label>
        <select value={pageSize} onChange={handlePageSizeChange} style={{ padding: 5, fontSize: '0.7rem' }}>
          {PAGE_SIZE_OPTIONS.map(size => (
            <option key={size} value={size}>{size}</option>
          ))}
        </select>
        <span style={{ marginLeft: 16 }}>
          Page: <b>{page + 1}</b> / <b>{totalPages}</b>
        </span>
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => handlePageChange(page - 1)} disabled={page <= 0} style={{ padding: '6px 12px', fontSize: '0.7rem', background: '#eee', border: '1px solid #ccc', borderRadius: 6, cursor: page <= 0 ? 'not-allowed' : 'pointer' }}>Prev</button>
          <button onClick={() => handlePageChange(page + 1)} disabled={page + 1 >= totalPages} style={{ padding: '6px 12px', fontSize: '0.7rem', background: '#eee', border: '1px solid #ccc', borderRadius: 6, cursor: page + 1 >= totalPages ? 'not-allowed' : 'pointer' }}>Next</button>
        </div>
      </div>
      <Pagination page={page} pageSize={pageSize} totalPages={totalPages} onPageChange={fetchMetrics} />
    </div>
  );
};

export default MetricsManagement;
