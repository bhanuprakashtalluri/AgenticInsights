import React, { useEffect, useState } from 'react';
import axios from 'axios';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];
const sortableColumns = [
  { key: 'points', label: 'Points' },
];

const TablePanel = ({
  title,
  data,
  loading,
  error,
  topN,
  setTopN,
  search,
  setSearch,
  sortField,
  setSortField,
  sortOrder,
  setSortOrder,
}: {
  title: string;
  data: any[];
  loading: boolean;
  error: string;
  topN: string;
  setTopN: (topN: string) => void;
  search: string;
  setSearch: (search: string) => void;
  sortField: string;
  setSortField: (field: string) => void;
  sortOrder: 'asc' | 'desc';
  setSortOrder: (order: 'asc' | 'desc') => void;
}) => {
  // Filter, sort, and slice
  let filtered = data;
  if (search.trim()) {
    const q = search.trim().toLowerCase();
    filtered = filtered.filter(e =>
      [e.name, e.points].some(val =>
        val !== undefined && val !== null && String(val).toLowerCase().includes(q)
      )
    );
  }
  filtered = [...filtered].sort((a, b) => {
    let aVal = a[sortField];
    let bVal = b[sortField];
    aVal = Number(aVal);
    bVal = Number(bVal);
    if (aVal < bVal) return sortOrder === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortOrder === 'asc' ? 1 : -1;
    return 0;
  });
  const n = parseInt(topN, 10);
  if (!isNaN(n) && n > 0) filtered = filtered.slice(0, n);

  return (
    <div style={{ flex: 1, minWidth: 340, maxWidth: 500, background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', padding: 18 }}>
      <h3 style={{ textAlign: 'center', fontSize: '1rem', fontWeight: 600, marginBottom: 12 }}>{title}</h3>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10, justifyContent: 'space-between' }}>
        <div style={{ flex: 1, display: 'flex', justifyContent: 'flex-start' }}>
          <input
            type="number"
            min={1}
            value={topN}
            onChange={e => setTopN(e.target.value.replace(/[^\d]/g, ''))}
            placeholder="Top N"
            style={{ padding: '6px 10px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 80 }}
          />
        </div>
        <div style={{ flex: 1, display: 'flex', justifyContent: 'flex-end' }}>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder={`Search ${title.toLowerCase()}...`}
            style={{ padding: '6px 10px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 140 }}
          />
        </div>
      </div>
      {loading ? (
        <div style={{ fontSize: '0.8rem', textAlign: 'center', margin: 12 }}>Loading...</div>
      ) : error ? (
        <div style={{ color: 'red', fontSize: '0.8rem', textAlign: 'center', margin: 12 }}>{error}</div>
      ) : (
        <table style={{ width: '100%', fontSize: '0.8rem', background: '#fff', borderRadius: 8 }}>
          <thead>
            <tr style={{ background: '#8da1bd' }}>
              <th style={{ padding: 8, textAlign: 'left', fontWeight: 600 }}>Name</th>
              {sortableColumns.map(col => (
                <th
                  key={col.key}
                  style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap', textAlign: 'left', fontWeight: 600 }}
                  onClick={() => {
                    if (sortField === col.key) {
                      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                    } else {
                      setSortField(col.key);
                      setSortOrder('asc');
                    }
                  }}
                >
                  {col.label}
                  <span style={{ marginLeft: 4, color: sortField === col.key ? '#000' : '#bbb', fontWeight: 700, fontSize: '1rem', cursor: 'pointer' }}>
                    {sortField === col.key ? (sortOrder === 'asc' ? '▲' : '▼') : '▲▼'}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={2} style={{ padding: 8, textAlign: 'center', color: '#888' }}>No data found.</td></tr>
            ) : filtered.map((row, idx) => (
              <tr key={row.name || row.id || idx} style={{ background: idx % 2 === 0 ? '#f5f7fa' : '#fff' }}>
                <td style={{ padding: 8 }}>{row.name}</td>
                <td style={{ padding: 8 }}>{row.points}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

const LeaderboardManagement: React.FC = () => {
  const [senders, setSenders] = useState<any[]>([]);
  const [receivers, setReceivers] = useState<any[]>([]);
  const [loadingSenders, setLoadingSenders] = useState(true);
  const [loadingReceivers, setLoadingReceivers] = useState(true);
  const [errorSenders, setErrorSenders] = useState('');
  const [errorReceivers, setErrorReceivers] = useState('');

  // Controls for senders
  const [topNSenders, setTopNSenders] = useState('');
  const [searchSenders, setSearchSenders] = useState('');
  const [sortFieldSenders, setSortFieldSenders] = useState('points');
  const [sortOrderSenders, setSortOrderSenders] = useState<'asc' | 'desc'>('desc');

  // Controls for receivers
  const [topNReceivers, setTopNReceivers] = useState('');
  const [searchReceivers, setSearchReceivers] = useState('');
  const [sortFieldReceivers, setSortFieldReceivers] = useState('points');
  const [sortOrderReceivers, setSortOrderReceivers] = useState<'asc' | 'desc'>('desc');

  useEffect(() => {
    const fetchSenders = async () => {
      setLoadingSenders(true);
      setErrorSenders('');
      try {
        const res = await axios.get('/leaderboard/top-senders?page=0&size=1000');
        setSenders(res.data.content || []);
      } catch (err) {
        setErrorSenders('Failed to fetch top senders');
        setSenders([]);
      } finally {
        setLoadingSenders(false);
      }
    };
    const fetchReceivers = async () => {
      setLoadingReceivers(true);
      setErrorReceivers('');
      try {
        const res = await axios.get('/leaderboard/top-recipients?page=0&size=1000');
        setReceivers(res.data.content || []);
      } catch (err) {
        setErrorReceivers('Failed to fetch top receivers');
        setReceivers([]);
      } finally {
        setLoadingReceivers(false);
      }
    };
    fetchSenders();
    fetchReceivers();
  }, []);

  return (
    <div style={{ width: '100%', minHeight: 400, display: 'flex', gap: 32, justifyContent: 'center', alignItems: 'flex-start', background: '#f5f7fa' }}>
      <TablePanel
        title="Top Senders"
        data={senders}
        loading={loadingSenders}
        error={errorSenders}
        topN={topNSenders}
        setTopN={setTopNSenders}
        search={searchSenders}
        setSearch={setSearchSenders}
        sortField={sortFieldSenders}
        setSortField={setSortFieldSenders}
        sortOrder={sortOrderSenders}
        setSortOrder={setSortOrderSenders}
      />
      <TablePanel
        title="Top Receivers"
        data={receivers}
        loading={loadingReceivers}
        error={errorReceivers}
        topN={topNReceivers}
        setTopN={setTopNReceivers}
        search={searchReceivers}
        setSearch={setSearchReceivers}
        sortField={sortFieldReceivers}
        setSortField={setSortFieldReceivers}
        sortOrder={sortOrderReceivers}
        setSortOrder={setSortOrderReceivers}
      />
    </div>
  );
};

export default LeaderboardManagement;
