import React, { useEffect, useState } from 'react';
import axios from 'axios';

const EmployeeManagement: React.FC = () => {
  const [employees, setEmployees] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    role: 'employee',
    unitId: '',
    managerId: '',
    joiningDate: ''
  });
  const [creating, setCreating] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<any>(null);
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState('');
  const [editSuccess, setEditSuccess] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);

  // Sorting state
  const [sortField, setSortField] = useState<string>('firstName');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

  // Search state
  const [search, setSearch] = useState('');

  // Store all employees for global search/filtering
  const [allEmployees, setAllEmployees] = useState<any[]>([]);

  const fetchEmployees = async (newPage = page, newPageSize = pageSize) => {
    setLoading(true);
    setError('');
    try {
      const res = await axios.get(`/employees?page=${newPage}&size=${newPageSize}`);
      setEmployees(res.data.content || []);
      setTotalPages(res.data.totalPages || 1);
      setPage(res.data.pageable?.pageNumber ?? newPage);
      setPageSize(res.data.pageable?.pageSize ?? newPageSize);
    } catch (err: any) {
      setError('Failed to fetch employees');
    } finally {
      setLoading(false);
    }
  };

  // Fetch all employees for search/filtering
  const fetchAllEmployees = async () => {
    try {
      const res = await axios.get('/employees?page=0&size=10000');
      setAllEmployees(res.data.content || []);
    } catch (err) {
      setAllEmployees([]);
    }
  };

  useEffect(() => {
    fetchEmployees();
    fetchAllEmployees();
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement> | React.ChangeEvent<HTMLSelectElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setError('');
    setSuccess('');
    try {
      await axios.post('/employees', form);
      setSuccess('Employee created successfully');
      setForm({ firstName: '', lastName: '', email: '', role: 'employee', unitId: '', managerId: '', joiningDate: '' });
      fetchEmployees();
    } catch (err: any) {
      setError('Failed to create employee');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number) => {
    setError('');
    setSuccess('');
    try {
      await axios.delete(`/employees/single?id=${id}`);
      setSuccess('Employee deleted successfully');
      fetchEmployees();
    } catch (err: any) {
      setError('Failed to delete employee');
    }
  };

  const startEdit = (emp: any) => {
    setEditingId(emp.id);
    setEditForm({
      firstName: emp.firstName || '',
      lastName: emp.lastName || '',
      email: emp.email || '',
      role: emp.role || 'employee',
      unitId: emp.unitId || '',
      managerId: emp.managerId || '',
      joiningDate: emp.joiningDate || ''
    });
    setEditError('');
    setEditSuccess('');
  };

  const handleEditInputChange = (e: React.ChangeEvent<HTMLInputElement> | React.ChangeEvent<HTMLSelectElement>) => {
    setEditForm({ ...editForm, [e.target.name]: e.target.value });
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editingId == null) return;
    setEditLoading(true);
    setEditError('');
    setEditSuccess('');
    try {
      await axios.put(`/employees/single?id=${editingId}`, editForm);
      setEditSuccess('Employee updated successfully');
      setEditingId(null);
      setEditForm(null);
      fetchEmployees();
    } catch (err: any) {
      setEditError('Failed to update employee');
    } finally {
      setEditLoading(false);
    }
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditForm(null);
    setEditError('');
    setEditSuccess('');
  };

  const handlePageChange = (newPage: number) => {
    fetchEmployees(newPage, pageSize);
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPageSize(newSize);
    fetchEmployees(0, newSize);
  };

  // Filter employees by search query (global)
  const getFilteredEmployees = () => {
    const source = search.trim() ? allEmployees : employees;
    if (!search.trim()) return source;
    const q = search.trim().toLowerCase();
    return source.filter(emp =>
      [emp.firstName, emp.lastName, emp.email, emp.role, emp.unitId, emp.managerId, emp.joiningDate]
        .map(val => (val ? String(val).toLowerCase() : ''))
        .some(val => val.includes(q))
    );
  };

  // Sorting logic (use filtered employees)
  const getSortedEmployees = () => {
    return [...getFilteredEmployees()].sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];
      if (aVal === undefined || aVal === null) aVal = '';
      if (bVal === undefined || bVal === null) bVal = '';
      // Numeric sort for managerId/unitId
      if ((sortField === 'managerId' || sortField === 'unitId') && !isNaN(Number(aVal)) && !isNaN(Number(bVal))) {
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

  // Paging logic for filtered/sorted employees
  const getPagedEmployees = () => {
    const sorted = getSortedEmployees();
    const total = Math.max(1, Math.ceil(sorted.length / pageSize));
    if (totalPages !== total) setTotalPages(total);
    return sorted.slice(page * pageSize, (page + 1) * pageSize);
  };

  if (loading) return <div>Loading employees...</div>;
  return (
    <div style={{ width: '100%', minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 24, fontSize: '1.7rem', fontWeight: 600 }}>Employee Management</h2>
      {error && <div style={{ color: 'red', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{error}</div>}
      {success && <div style={{ color: 'green', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{success}</div>}
      <form onSubmit={handleCreate} style={{ width: '100%', display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20, alignItems: 'center', background: '#fff', padding: 10, borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', fontSize: '0.7rem', justifyContent: 'center' }}>
        <input name="firstName" placeholder="First Name" value={form.firstName} onChange={handleInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <input name="lastName" placeholder="Last Name" value={form.lastName} onChange={handleInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <input name="email" placeholder="Email" value={form.email} onChange={handleInputChange} required style={{ flex: '2 1 160px', fontSize: '0.7rem', padding: 6 }} />
        <select name="role" value={form.role} onChange={handleInputChange} required style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }}>
          <option value="employee">Employee</option>
          <option value="teamleader">Teamleader</option>
          <option value="manager">Manager</option>
        </select>
        <input name="unitId" placeholder="Unit ID" value={form.unitId} onChange={handleInputChange} style={{ flex: '1 1 70px', fontSize: '0.7rem', padding: 6 }} />
        <input name="managerId" placeholder="Manager ID" value={form.managerId} onChange={handleInputChange} style={{ flex: '1 1 70px', fontSize: '0.7rem', padding: 6 }} />
        <input name="joiningDate" placeholder="Joining Date (YYYY-MM-DD)" value={form.joiningDate} onChange={handleInputChange} style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }} />
        <div style={{ flex: '1 1 100%', display: 'flex', justifyContent: 'center', gap: 10 }}>
          <button type="submit" disabled={creating} style={{ background: '#1976d2', color: '#fff', border: 'none', borderRadius: 6, padding: '7px 14px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }}>Create</button>
        </div>
      </form>
      <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 14, fontSize: '0.7rem', justifyContent: 'center', width: '100%' }}>
        <label>Page Size:</label>
        <select value={pageSize} onChange={handlePageSizeChange} style={{ padding: 5, fontSize: '0.7rem' }}>
          {[5, 10, 20, 50, 100].map(size => (
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
      <div style={{ width: '100%', overflowX: 'auto' }}>
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 8 }}>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search employees..."
            style={{ padding: '7px 12px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 220 }}
          />
        </div>
        <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', fontSize: '0.7rem' }}>
          <thead>
            <tr style={{ background: '#8da1bd' }}>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('firstName');
                    setSortOrder(sortField === 'firstName' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                First Name
                <span style={{ marginLeft: 4, color: sortField === 'firstName' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('firstName');
                        setSortOrder(sortField === 'firstName' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'firstName' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('lastName');
                    setSortOrder(sortField === 'lastName' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                Last Name
                <span style={{ marginLeft: 4, color: sortField === 'lastName' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('lastName');
                        setSortOrder(sortField === 'lastName' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'lastName' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('email');
                    setSortOrder(sortField === 'email' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                Email
                <span style={{ marginLeft: 4, color: sortField === 'email' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('email');
                        setSortOrder(sortField === 'email' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'email' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('role');
                    setSortOrder(sortField === 'role' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                Role
                <span style={{ marginLeft: 4, color: sortField === 'role' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('role');
                        setSortOrder(sortField === 'role' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'role' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('unitId');
                    setSortOrder(sortField === 'unitId' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                Unit
                <span style={{ marginLeft: 4, color: sortField === 'unitId' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('unitId');
                        setSortOrder(sortField === 'unitId' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'unitId' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('managerId');
                    setSortOrder(sortField === 'managerId' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                Manager
                <span style={{ marginLeft: 4, color: sortField === 'managerId' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('managerId');
                        setSortOrder(sortField === 'managerId' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'managerId' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8, cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
                  onClick={() => {
                    setSortField('joiningDate');
                    setSortOrder(sortField === 'joiningDate' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                  }}>
                Joining Date
                <span style={{ marginLeft: 4, color: sortField === 'joiningDate' ? '#222' : '#bbb', cursor: 'pointer' }}
                      onClick={e => {
                        e.stopPropagation();
                        setSortField('joiningDate');
                        setSortOrder(sortField === 'joiningDate' ? (sortOrder === 'asc' ? 'desc' : 'asc') : 'asc');
                      }}>
                  {sortField === 'joiningDate' ? (sortOrder === 'asc' ? '▲' : '▼') : (sortOrder === 'asc' ? '▲' : '▼')}
                </span>
              </th>
              <th style={{ padding: 8 }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {getPagedEmployees().map(emp => (
              <tr key={emp.id} style={{ background: editingId === emp.id ? '#e3f2fd' : '#fff' }}>
                {editingId === emp.id ? (
                  <td colSpan={8} style={{ padding: 0 }}>
                    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', width: '100%' }}>
                      <form onSubmit={handleEditSubmit} style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.7rem', width: '100%', justifyContent: 'center' }}>
                        <input name="firstName" value={editForm.firstName} onChange={handleEditInputChange} required style={{ fontSize: '0.7rem', padding: 6 }} />
                        <input name="lastName" value={editForm.lastName} onChange={handleEditInputChange} required style={{ fontSize: '0.7rem', padding: 6 }} />
                        <input name="email" value={editForm.email} onChange={handleEditInputChange} required style={{ fontSize: '0.7rem', padding: 6 }} />
                        <select name="role" value={editForm.role} onChange={handleEditInputChange} required style={{ fontSize: '0.7rem', padding: 6 }}>
                          <option value="employee">Employee</option>
                          <option value="teamleader">Teamleader</option>
                          <option value="manager">Manager</option>
                        </select>
                        <input name="unitId" value={editForm.unitId} onChange={handleEditInputChange} style={{ fontSize: '0.7rem', padding: 6 }} />
                        <input name="managerId" value={editForm.managerId} onChange={handleEditInputChange} style={{ fontSize: '0.7rem', padding: 6 }} />
                        <input name="joiningDate" value={editForm.joiningDate} onChange={handleEditInputChange} style={{ fontSize: '0.7rem', padding: 6 }} />
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button type="submit" disabled={editLoading} style={{ background: '#388e3c', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 12px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }}>Save</button>
                          <button type="button" onClick={cancelEdit} disabled={editLoading} style={{ background: '#eee', color: '#333', border: 'none', borderRadius: 6, padding: '6px 12px', fontSize: '0.7rem', cursor: 'pointer' }}>Cancel</button>
                        </div>
                        {editError && <span style={{ color: 'red', marginLeft: 6 }}>{editError}</span>}
                        {editSuccess && <span style={{ color: 'green', marginLeft: 6 }}>{editSuccess}</span>}
                      </form>
                    </div>
                  </td>
                ) : (
                  <>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.firstName}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.lastName}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.email}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.role}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.unitId}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.managerId}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>{emp.joiningDate}</td>
                    <td style={{ padding: 10, fontSize: '0.7rem' }}>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button style={{ background: '#d32f2f', color: '#fff', border: 'none', borderRadius: 6, padding: '5px 10px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }} onClick={() => handleDelete(emp.id)}>Delete</button>
                        <PromoteButton emp={emp} onPromote={fetchEmployees} />
                        <button style={{ background: '#1976d2', color: '#fff', border: 'none', borderRadius: 6, padding: '5px 10px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }} onClick={() => startEdit(emp)}>Edit</button>
                      </div>
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const PromoteButton: React.FC<{ emp: any; onPromote: () => void }> = ({ emp, onPromote }) => {
  const [promoting, setPromoting] = useState(false);
  const [newRole, setNewRole] = useState(emp.role);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handlePromote = async () => {
    setPromoting(true);
    setError('');
    setSuccess('');
    try {
      await axios.put(`/employees/single?id=${emp.id}`, { ...emp, role: newRole });
      setSuccess('Role updated');
      onPromote();
    } catch (err: any) {
      setError('Failed to promote');
    } finally {
      setPromoting(false);
    }
  };

  return (
    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <select value={newRole} onChange={e => setNewRole(e.target.value)} disabled={promoting} style={{ fontSize: '0.7rem', padding: '4px 8px', borderRadius: 4 }}>
        <option value="employee">Employee</option>
        <option value="teamleader">Teamleader</option>
        <option value="manager">Manager</option>
      </select>
      <button onClick={handlePromote} disabled={promoting || newRole === emp.role} style={{ background: '#ffa000', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 16px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }}>Promote</button>
      {error && <span style={{ color: 'red', marginLeft: 4 }}>{error}</span>}
      {success && <span style={{ color: 'green', marginLeft: 4 }}>{success}</span>}
    </span>
  );
};

export default EmployeeManagement;
