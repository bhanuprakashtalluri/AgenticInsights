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

  useEffect(() => {
    fetchTypes();
  }, []);

  const handlePageChange = (newPage: number) => {
    fetchTypes(newPage, pageSize);
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPageSize(newSize);
    fetchTypes(0, newSize);
  };

  return (
    <div style={{ width: '100%', overflowX: 'auto' }}>
      <h3 style={{ textAlign: 'center', fontSize: '1rem', fontWeight: 600, marginBottom: 12 }}>Recognition Types</h3>
      {loading && <div style={{ fontSize: '0.7rem' }}>Loading recognition types...</div>}
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
            <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}>
              Type Name
              <span style={{ marginLeft: 4, color: '#bbb' }}>▲▼</span>
            </th>
            <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}>
              Description
              <span style={{ marginLeft: 4, color: '#bbb' }}>▲▼</span>
            </th>
            <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}>
              Default Points
              <span style={{ marginLeft: 4, color: '#bbb' }}>▲▼</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {types.map(type => (
            <tr key={type.id} style={{ background: '#fff' }}>
              <td style={{ padding: 8 }}>{type.typeName}</td>
              <td style={{ padding: 8 }}>{type.description || '-'}</td>
              <td style={{ padding: 8 }}>{type.defaultPoints || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!loading && !error && types.length === 0 && (
        <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No recognition types found.</div>
      )}
    </div>
  );
};

export default RecognitionTypeManagement;