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

  // Search state
  const [search, setSearch] = useState('');
  // Create form state
  const [createForm, setCreateForm] = useState({
    recognitionTypeName: '',
    category: '',
    level: '',
    awardPoints: '',
    senderName: '',
    recipientName: '',
    message: '',
  });
  const [creating, setCreating] = useState(false);
  const [createSuccess, setCreateSuccess] = useState('');
  const [createError, setCreateError] = useState('');

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

  // Ensure table updates on sort/page change if recognitions are loaded
  useEffect(() => {
    if (allRecognitions.length > 0) {
      updateFrontendPage(page, pageSize);
    }
    // eslint-disable-next-line
  }, [sortField, sortOrder, page, pageSize, allRecognitions]);

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

  // Filter recognitions by search query (searches allRecognitions if search is present)
  const getFilteredRecognitions = () => {
    const source = search.trim() ? allRecognitions : recognitions;
    if (!search.trim()) return recognitions;
    const q = search.trim().toLowerCase();
    return source.filter(rec =>
      [rec.id, rec.recognitionTypeName, rec.category, rec.level, rec.awardPoints, rec.senderName, rec.recipientName, rec.message, rec.approvalStatus, rec.rejectionReason]
        .map(val => (val !== undefined && val !== null ? String(val).toLowerCase() : ''))
        .some(val => val.includes(q))
    );
  };

  // Get paged recognitions (applies paging to filtered results)
  const getPagedRecognitions = () => {
    const filtered = getFilteredRecognitions();
    const total = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (totalPages !== total) setTotalPages(total);
    return filtered.slice(page * pageSize, (page + 1) * pageSize);
  };

  // Create recognition handler
  const handleCreateInputChange = (e: React.ChangeEvent<HTMLInputElement> | React.ChangeEvent<HTMLSelectElement>) => {
    setCreateForm({ ...createForm, [e.target.name]: e.target.value });
  };
  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setCreateError('');
    setCreateSuccess('');
    try {
      // You may need to map names to IDs/UUIDs in a real app
      await axios.post('/recognitions', {
        recognitionTypeName: createForm.recognitionTypeName,
        category: createForm.category,
        level: createForm.level,
        awardPoints: Number(createForm.awardPoints),
        senderName: createForm.senderName,
        recipientName: createForm.recipientName,
        message: createForm.message,
      });
      setCreateSuccess('Recognition created successfully');
      setCreateForm({ recognitionTypeName: '', category: '', level: '', awardPoints: '', senderName: '', recipientName: '', message: '' });
      // Reload recognitions
      setTimeout(() => window.location.reload(), 500); // quick reload for demo
    } catch (err: any) {
      setCreateError('Failed to create recognition');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div style={{ width: '100%', minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 24, fontSize: '1.7rem', fontWeight: 600 }}>Recognition Management</h2>
      {error && <div style={{ color: 'red', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{error}</div>}
      {createError && <div style={{ color: 'red', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{createError}</div>}
      {createSuccess && <div style={{ color: 'green', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{createSuccess}</div>}
      <form onSubmit={handleCreate} style={{ width: '100%', display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20, alignItems: 'center', background: '#fff', padding: 10, borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', fontSize: '0.7rem', justifyContent: 'center' }}>
        <input name="recognitionTypeName" placeholder="Type" value={createForm.recognitionTypeName} onChange={handleCreateInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <input name="category" placeholder="Category" value={createForm.category} onChange={handleCreateInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <input name="level" placeholder="Level" value={createForm.level} onChange={handleCreateInputChange} style={{ flex: '1 1 70px', fontSize: '0.7rem', padding: 6 }} />
        <input name="awardPoints" placeholder="Points" value={createForm.awardPoints} onChange={handleCreateInputChange} style={{ flex: '1 1 70px', fontSize: '0.7rem', padding: 6 }} />
        <input name="senderName" placeholder="Sender Name" value={createForm.senderName} onChange={handleCreateInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <input name="recipientName" placeholder="Recipient Name" value={createForm.recipientName} onChange={handleCreateInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <input name="message" placeholder="Message" value={createForm.message} onChange={handleCreateInputChange} required style={{ flex: '2 1 160px', fontSize: '0.7rem', padding: 6 }} />
        <div style={{ flex: '1 1 100%', display: 'flex', justifyContent: 'center', gap: 10 }}>
          <button type="submit" disabled={creating} style={{ background: '#1976d2', color: '#fff', border: 'none', borderRadius: 6, padding: '7px 14px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }}>Create</button>
        </div>
      </form>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 8, width: '100%' }}>
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search recognitions..."
          style={{ padding: '7px 12px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 220 }}
        />
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
                }}
              >
                {col.label}
                <span style={{ marginLeft: 4, color: sortField === col.key ? '#050505' : '#bbb', fontWeight: 700, fontSize: '1rem', cursor: 'pointer' }}>
                  {sortField === col.key ? (sortOrder === 'asc' ? '▲' : '▼') : '▲▼'}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {getPagedRecognitions().map(rec => (
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
      {!loading && !error && getPagedRecognitions().length === 0 && (
        <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No recognitions found.</div>
      )}
    </div>
  );
};

export default RecognitionManagement;
