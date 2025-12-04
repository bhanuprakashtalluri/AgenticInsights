import React, { useState } from 'react';
import Pagination from '../components/Pagination';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];

const LeaderboardManagement: React.FC = () => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);

  // Example leaderboard data, replace with real API data as needed
  const leaderboard = [
    { name: 'Alice', points: 120 },
    { name: 'Bob', points: 110 },
    { name: 'Charlie', points: 90 },
  ];

  const fetchLeaderboard = (newPage: number, newPageSize: number) => {
    setPage(newPage);
    setPageSize(newPageSize);
    // If you fetch paginated leaderboard, update totalPages here
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    // If you fetch paginated leaderboard, call fetchLeaderboard(newPage, pageSize)
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPageSize(newSize);
    setPage(0);
    // If you fetch paginated leaderboard, call fetchLeaderboard(0, newSize)
  };

  return (
    <div style={{ width: '100%', padding: 24 }}>
      <h3 style={{ textAlign: 'center', fontSize: '1.2rem', fontWeight: 600, marginBottom: 24 }}>Leaderboard</h3>
      <table style={{ width: '100%', fontSize: '0.7rem', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0' }}>
        <thead>
          <tr style={{ background: '#8da1bd' }}>
            <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}>
              Name
              <span style={{ marginLeft: 4, color: '#bbb' }}>▲▼</span>
            </th>
            <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}>
              Points
              <span style={{ marginLeft: 4, color: '#bbb' }}>▲▼</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {leaderboard.map((entry, idx) => (
            <tr key={idx} style={{ background: '#fff' }}>
              <td style={{ padding: 8 }}>{entry.name}</td>
              <td style={{ padding: 8 }}>{entry.points}</td>
            </tr>
          ))}
        </tbody>
      </table>
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
      <Pagination page={page} pageSize={pageSize} totalPages={totalPages} onPageChange={fetchLeaderboard} />
    </div>
  );
};

export default LeaderboardManagement;
