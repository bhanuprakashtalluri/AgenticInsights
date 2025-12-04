import React, { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';

const SIDEBAR_WIDTH = 180;

const LeaderboardPage: React.FC = () => {
  const [senders, setSenders] = useState<any[]>([]);
  const [receivers, setReceivers] = useState<any[]>([]);
  const [loadingSenders, setLoadingSenders] = useState(true);
  const [loadingReceivers, setLoadingReceivers] = useState(true);
  const [errorSenders, setErrorSenders] = useState('');
  const [errorReceivers, setErrorReceivers] = useState('');

  useEffect(() => {
    const fetchSenders = async () => {
      setLoadingSenders(true);
      setErrorSenders('');
      try {
        const res = await fetch('/leaderboard/top-senders?page=0&size=10');
        if (!res.ok) throw new Error('Failed to fetch top senders: ' + res.status + ' ' + res.statusText);
        const data = await res.json();
        setSenders(data.content || []);
      } catch (err) {
        console.error('Top senders fetch error:', err);
        setErrorSenders('Failed to fetch top senders from the backend.');
        setSenders([]);
      } finally {
        setLoadingSenders(false);
      }
    };
    const fetchReceivers = async () => {
      setLoadingReceivers(true);
      setErrorReceivers('');
      try {
        const res = await fetch('/leaderboard/top-recipients?page=0&size=10');
        if (!res.ok) throw new Error('Failed to fetch top receivers: ' + res.status + ' ' + res.statusText);
        const data = await res.json();
        setReceivers(data.content || []);
      } catch (err) {
        console.error('Top receivers fetch error:', err);
        setErrorReceivers('Failed to fetch top receivers from the backend.');
        setReceivers([]);
      } finally {
        setLoadingReceivers(false);
      }
    };
    fetchSenders();
    fetchReceivers();
  }, []);

  // Sort senders and receivers by points descending before rendering
  const sortedSenders = [...senders].sort((a, b) => (b.points ?? 0) - (a.points ?? 0));
  const sortedReceivers = [...receivers].sort((a, b) => (b.points ?? 0) - (a.points ?? 0));

  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: '#f5f7fa' }}>
      <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, height: '100vh', position: 'relative', zIndex: 2 }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', minHeight: '100vh' }}>
        <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600 }}>Leaderboard</h2>
        <div style={{ display: 'flex', width: '100%', gap: 32, justifyContent: 'center', alignItems: 'flex-start' }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <h3 style={{ textAlign: 'center', fontSize: '1rem', marginBottom: 8 }}>Top Senders</h3>
            {loadingSenders && <div style={{ fontSize: '0.7rem' }}>Loading...</div>}
            {errorSenders && <div style={{ color: 'red', fontSize: '0.7rem', marginBottom: 8 }}>{errorSenders}</div>}
            <table style={{ width: '100%', fontSize: '0.7rem', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', marginTop: 12 }}>
              <thead>
                <tr style={{ background: '#8da1bd' }}>
                  <th style={{ padding: 8 }}>Name</th>
                  <th style={{ padding: 8 }}>Points</th>
                </tr>
              </thead>
              <tbody>
                {sortedSenders.map((row, idx) => (
                  <tr key={row.name || row.id || idx} style={{ background: idx % 2 === 0 ? '#f5f7fa' : '#fff' }}>
                    <td style={{ padding: 8 }}>{typeof row.name === 'string' || typeof row.name === 'number' ? row.name : JSON.stringify(row.name)}</td>
                    <td style={{ padding: 8 }}>{typeof row.points === 'string' || typeof row.points === 'number' ? row.points : JSON.stringify(row.points)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!loadingSenders && !errorSenders && senders.length === 0 && (
              <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No senders found.</div>
            )}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <h3 style={{ textAlign: 'center', fontSize: '1rem', marginBottom: 8 }}>Top Receivers</h3>
            {loadingReceivers && <div style={{ fontSize: '0.7rem' }}>Loading...</div>}
            {errorReceivers && <div style={{ color: 'red', fontSize: '0.7rem', marginBottom: 8 }}>{errorReceivers}</div>}
            <table style={{ width: '100%', fontSize: '0.7rem', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', marginTop: 12 }}>
              <thead>
                <tr style={{ background: '#8da1bd' }}>
                  <th style={{ padding: 8 }}>Name</th>
                  <th style={{ padding: 8 }}>Points</th>
                </tr>
              </thead>
              <tbody>
                {sortedReceivers.map((row, idx) => (
                  <tr key={row.name || row.id || idx} style={{ background: idx % 2 === 0 ? '#f5f7fa' : '#fff' }}>
                    <td style={{ padding: 8 }}>{typeof row.name === 'string' || typeof row.name === 'number' ? row.name : JSON.stringify(row.name)}</td>
                    <td style={{ padding: 8 }}>{typeof row.points === 'string' || typeof row.points === 'number' ? row.points : JSON.stringify(row.points)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!loadingReceivers && !errorReceivers && receivers.length === 0 && (
              <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No receivers found.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default LeaderboardPage;
