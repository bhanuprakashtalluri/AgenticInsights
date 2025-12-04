import React, { useEffect, useState } from 'react';
import axios from 'axios';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];

const RecognitionManagement: React.FC = () => {
  const [recognitions, setRecognitions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [sortField, setSortField] = useState('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [allRecognitions, setAllRecognitions] = useState<any[]>([]); // Store all recognitions if backend returns array

  const sortableColumns = [
    { key: 'id', label: 'ID' },
    { key: 'recognitionTypeName', label: 'Type' },
    { key: 'category', label: 'Category' },
    { key: 'level', label: 'Level' },
    { key: 'awardPoints', label: 'Points' },
    { key: 'senderName', label: 'Sender Name' },
    { key: 'recipientName', label: 'Recipient Name' },
    { key: 'message', label: 'Message' },
    { key: 'sentAt', label: 'Date' },
    { key: 'approvalStatus', label: 'Approval Status' },
    { key: 'rejectionReason', label: 'Rejection Reason' },
  ];

  const sortRecognitions = (data: any[]) => {
    return [...data].sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];
      // Special handling for date
      if (sortField === 'sentAt') {
        aVal = a.sentAt || a.createdAt || 0;
        bVal = b.sentAt || b.createdAt || 0;
      }
      // Handle null/undefined
      if (aVal === undefined || aVal === null) aVal = '';
      if (bVal === undefined || bVal === null) bVal = '';
      // Numeric sort if both are numbers
      if (!isNaN(Number(aVal)) && !isNaN(Number(bVal))) {
        aVal = Number(aVal);
        bVal = Number(bVal);
      } else {
        aVal = String(aVal).toLowerCase();
        bVal = String(bVal).toLowerCase();
      }
      if (aVal < bVal) return sortOrder === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });
  };

  const fetchRecognitions = async (newPage = page, newPageSize = pageSize) => {
    setLoading(true);
    setError('');
    try {
      if (newPageSize < 1) newPageSize = 1;
      if (newPageSize > 100) newPageSize = 100;
      const res = await axios.get(`/recognitions?page=${newPage}&size=${newPageSize}`);
      let items;
      let total = 1;
      if (Array.isArray(res.data)) {
        // Frontend paging for array response
        setAllRecognitions(res.data);
        total = Math.max(1, Math.ceil(res.data.length / newPageSize));
        items = sortRecognitions(res.data).slice(newPage * newPageSize, (newPage + 1) * newPageSize);
      } else {
        items = sortRecognitions(res.data.content || []);
        total = res.data.totalPages || 1;
        setAllRecognitions([]); // Clear if backend is paginated
      }
      setRecognitions(items);
      setTotalPages(total);
      setPage(newPage);
      setPageSize(newPageSize);
    } catch (err: any) {
      setError('Failed to fetch recognitions from the backend. Please check if the backend is running and the endpoint is correct.');
      setRecognitions([]);
      setTotalPages(1);
      setAllRecognitions([]);
    } finally {
      setLoading(false);
    }
  };

  // Helper for frontend paging
  const updateFrontendPage = (newPage: number, newPageSize: number) => {
    const total = Math.max(1, Math.ceil(allRecognitions.length / newPageSize));
    const items = sortRecognitions(allRecognitions).slice(newPage * newPageSize, (newPage + 1) * newPageSize);
    setRecognitions(items);
    setTotalPages(total);
    setPage(newPage);
    setPageSize(newPageSize);
  };

  // Initial load only
  useEffect(() => {
    const loadRecognitions = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await axios.get(`/recognitions?page=0&size=10000`); // Get all for frontend paging
        if (Array.isArray(res.data)) {
          setAllRecognitions(res.data);
          updateFrontendPage(0, pageSize);
        } else {
          setAllRecognitions([]);
          setRecognitions(sortRecognitions(res.data.content || []));
          setTotalPages(res.data.totalPages || 1);
          setPage(res.data.pageable?.pageNumber ?? 0);
          setPageSize(res.data.pageable?.pageSize ?? pageSize);
        }
      } catch (err: any) {
        setError('Failed to fetch recognitions from the backend. Please check if the backend is running and the endpoint is correct.');
        setRecognitions([]);
        setTotalPages(1);
        setAllRecognitions([]);
      } finally {
        setLoading(false);
      }
    };
    loadRecognitions();
    // eslint-disable-next-line
  }, []);

  // Page change handler
  const handlePageChange = (newPage: number) => {
    if (allRecognitions.length > 0) {
      updateFrontendPage(newPage, pageSize);
    } else {
      fetchRecognitions(newPage, pageSize);
    }
  };

  // Page size change handler
  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    if (allRecognitions.length > 0) {
      updateFrontendPage(0, newSize);
    } else {
      fetchRecognitions(0, newSize);
    }
  };

  return (
    <div style={{ width: '100%', overflowX: 'auto' }}>
      <h3 style={{ textAlign: 'center', fontSize: '1rem', fontWeight: 600, marginBottom: 12 }}>Recognitions List</h3>
      {loading && <div style={{ fontSize: '0.7rem' }}>Loading recognitions...</div>}
      {error && <div style={{ color: 'red', fontSize: '0.7rem', marginBottom: 8 }}>{error}</div>}
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
      <table style={{ width: '100%', fontSize: '0.7rem', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0' }}>
        <thead>
          <tr style={{ background: '#8da1bd' }}>
            {sortableColumns.map(col => (
              <th
                key={col.key}
                style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                onClick={() => {
                  if (sortField === col.key) {
                    setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                  } else {
                    setSortField(col.key);
                    setSortOrder('asc');
                  }
                  // Re-sort and update page
                  if (allRecognitions.length > 0) {
                    updateFrontendPage(page, pageSize);
                  } else {
                    fetchRecognitions(page, pageSize);
                  }
                }}
              >
                {col.label}
                <span style={{ marginLeft: 4, color: sortField === col.key ? '#222' : '#bbb' }}>
                  {sortField === col.key ? (sortOrder === 'asc' ? '▲' : '▼') : '▲▼'}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {recognitions.map(rec => (
            <tr key={rec.id} style={{ background: '#fff' }}>
              <td style={{ padding: 8 }}>{rec.id}</td>
              <td style={{ padding: 8 }}>{rec.recognitionTypeName || '-'}</td>
              <td style={{ padding: 8 }}>{rec.category}</td>
              <td style={{ padding: 8 }}>{rec.level || '-'}</td>
              <td style={{ padding: 8 }}>{rec.awardPoints}</td>
              <td style={{ padding: 8 }}>{rec.senderName || '-'}</td>
              <td style={{ padding: 8 }}>{rec.recipientName || '-'}</td>
              <td style={{ padding: 8 }}>{rec.message || '-'}</td>
              <td style={{ padding: 8 }}>{rec.sentAt ? new Date(rec.sentAt * 1000).toLocaleDateString() : '-'}</td>
              <td style={{ padding: 8 }}>{rec.approvalStatus || '-'}</td>
              <td style={{ padding: 8 }}>{rec.rejectionReason || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!loading && !error && recognitions.length === 0 && (
        <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No recognitions found.</div>
      )}
    </div>
  );
};

export default RecognitionManagement;
