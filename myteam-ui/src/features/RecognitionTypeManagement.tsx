import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Pagination from '../components/Pagination';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];

const RecognitionTypeManagement: React.FC = () => {
  const [types, setTypes] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);

  const sortableColumns = [
    { key: 'id', label: 'ID' },
    { key: 'typeName', label: 'Type Name' },
    { key: 'createdBy', label: 'Created By' },
    { key: 'createdAt', label: 'Created At' },
  ];
  const [allTypes, setAllTypes] = useState<any[]>([]);
  const [sortField, setSortField] = useState('typeName');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [search, setSearch] = useState('');

  const [createForm, setCreateForm] = useState({ typeName: '' });
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createSuccess, setCreateSuccess] = useState('');

  const fetchTypes = async (newPage = page, newPageSize = pageSize) => {
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      if (newPageSize < 1) newPageSize = 1;
      if (newPageSize > 100) newPageSize = 100;
      const res = await axios.get(`/recognition-types?page=${newPage}&size=${newPageSize}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setTypes(res.data.content || []);
      setTotalPages(res.data.totalPages || 1);
      setPage(res.data.pageable?.pageNumber ?? newPage);
      setPageSize(res.data.pageable?.pageSize ?? newPageSize);
    } catch (err: any) {
      setError('Failed to fetch recognition types');
    } finally {
      setLoading(false);
    }
  };

  const fetchAllTypes = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await axios.get('/recognition-types?page=0&size=10000', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAllTypes(res.data.content || []);
    } catch (err) {
      setAllTypes([]);
    }
  };

  useEffect(() => {
    fetchTypes();
    fetchAllTypes();
  }, []);

  const sortTypes = (data: any[]) => {
    return [...data].sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];
      if (aVal === undefined || aVal === null) aVal = '';
      if (bVal === undefined || bVal === null) bVal = '';
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

  const getFilteredTypes = () => {
    const source = search.trim() ? allTypes : types;
    if (!search.trim()) return types;
    const q = search.trim().toLowerCase();
    return source.filter(type =>
      [type.id, type.typeName, type.createdBy, type.createdAt]
        .map(val => (val !== undefined && val !== null ? String(val).toLowerCase() : ''))
        .some(val => val.includes(q))
    );
  };

  const getPagedTypes = () => {
    const filtered = sortTypes(getFilteredTypes());
    const total = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (totalPages !== total) setTotalPages(total);
    return filtered.slice(page * pageSize, (page + 1) * pageSize);
  };

  const handlePageChange = (newPage: number) => {
    fetchTypes(newPage, pageSize);
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPageSize(newSize);
    fetchTypes(0, newSize);
  };

  const handleCreateInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCreateForm({ ...createForm, [e.target.name]: e.target.value });
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setCreateError('');
    setCreateSuccess('');
    try {
      const token = localStorage.getItem('token');
      await axios.post('/recognition-types', { typeName: createForm.typeName }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setCreateSuccess('Recognition type created successfully');
      setCreateForm({ typeName: '' });
      fetchTypes();
      fetchAllTypes();
    } catch (err: any) {
      setCreateError('Failed to create recognition type');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div style={{ width: '100%', overflowX: 'auto' }}>
      <h3 style={{ textAlign: 'center', fontSize: '1rem', fontWeight: 600, marginBottom: 12 }}>Recognition Types</h3>
      <form onSubmit={handleCreate} style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12, justifyContent: 'center' }}>
        <input
          type="text"
          name="typeName"
          value={createForm.typeName}
          onChange={handleCreateInputChange}
          placeholder="New recognition type name"
          style={{ padding: '7px 12px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 220 }}
          required
        />
        <button type="submit" disabled={creating || !createForm.typeName.trim()} style={{ padding: '7px 18px', fontSize: '0.8rem', borderRadius: 6, background: '#2d6cdf', color: '#fff', border: 'none', fontWeight: 600, cursor: creating ? 'not-allowed' : 'pointer' }}>
          {creating ? 'Creating...' : 'Create'}
        </button>
      </form>
      {createError && <div style={{ color: 'red', fontSize: '0.7rem', marginBottom: 8, textAlign: 'center' }}>{createError}</div>}
      {createSuccess && <div style={{ color: 'green', fontSize: '0.7rem', marginBottom: 8, textAlign: 'center' }}>{createSuccess}</div>}
      {loading && <div style={{ fontSize: '0.7rem' }}>Loading recognition types...</div>}
      {error && <div style={{ color: 'red', fontSize: '0.7rem', marginBottom: 8 }}>{error}</div>}
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 8 }}>
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search recognition types..."
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
                <span style={{ marginLeft: 4, color: sortField === col.key ? '#000' : '#bbb', fontWeight: 700, fontSize: '1rem', cursor: 'pointer' }}>
                  {sortField === col.key ? (sortOrder === 'asc' ? '▲' : '▼') : '▲▼'}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {getPagedTypes().map(type => (
            <tr key={type.id} style={{ background: '#fff' }}>
              <td style={{ padding: 8 }}>{type.id}</td>
              <td style={{ padding: 8 }}>{type.typeName}</td>
              <td style={{ padding: 8 }}>{type.createdBy}</td>
              <td style={{ padding: 8 }}>{type.createdAt ? new Date(type.createdAt).toLocaleString() : '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!loading && !error && getPagedTypes().length === 0 && (
        <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No recognition types found.</div>
      )}
    </div>
  );
};

export default RecognitionTypeManagement;
