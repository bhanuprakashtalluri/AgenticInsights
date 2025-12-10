import React, { useEffect, useState, useRef } from 'react';
import axios from 'axios';
import { useAuth } from '../services/auth';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];

type RecognitionManagementProps = {
  showTable?: boolean;
};

const RecognitionManagement: React.FC<RecognitionManagementProps> = ({ showTable = true }) => {
  const { user } = useAuth();
  const [allRecognitions, setAllRecognitions] = useState<any[]>([]); // Store all recognitions for frontend filtering
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [sortField, setSortField] = useState('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  // Create form state
  const [createForm, setCreateForm] = useState({
    recognitionTypeName: '',
    category: '',
    level: '',
    awardPoints: '',
    recipientId: '',
    message: '',
  });
  const [creating, setCreating] = useState(false);
  const [createSuccess, setCreateSuccess] = useState('');
  const [createError, setCreateError] = useState('');

  // Filter state
  const [filterField, setFilterField] = useState<string | null>(null);
  const [filterValue, setFilterValue] = useState<string | null>(null);

  const [search, setSearch] = useState('');

  const [scopeNames, setScopeNames] = useState<string[]>([]);
  const norm = (v: any) => (typeof v === 'string' ? v.trim().toLowerCase() : v ?? '');

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

  const filterableKeys = ["recognitionTypeName", "category", "level", "approvalStatus"];
  const sortableArrowKeys = ["id", "awardPoints", "sentAt"];

  useEffect(() => {
    if (!user) return;
    axios.get('/employees?page=0&size=5000').then(res => {
      const list: any[] = Array.isArray(res.data) ? res.data : (res.data.content || []);
      const names = new Set<string>();
      const fullName = (e: any) => [e.firstName, e.lastName].filter(Boolean).join(' ').trim();
      if (user.role === 'employee') {
        const selfEmp = list.find((e: any) => norm(e.email) === norm(user.email));
        if (selfEmp) names.add(fullName(selfEmp));
      } else if (user.role === 'teamlead') {
        const tl = list.find((e: any) => norm(e.email) === norm(user.email));
        const managerId = tl?.id ?? tl?.employeeId ?? tl?.managerId ?? null;
        list.filter((e: any) => e.managerId === managerId).forEach((e: any) => names.add(fullName(e)));
        if (tl) names.add(fullName(tl));
      } else if (user.role === 'manager') {
        const mgr = list.find((e: any) => norm(e.email) === norm(user.email));
        const unitId = mgr?.unitId ?? user.unitId;
        list.filter((e: any) => String(e.unitId) === String(unitId)).forEach((e: any) => names.add(fullName(e)));
        if (mgr) names.add(fullName(mgr));
      }
      setScopeNames(Array.from(names));
    }).catch(() => {
      setScopeNames([]);
    });
  }, [user]);

  const fetchAllRecognitions = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await axios.get(`/recognitions?page=0&size=10000`);
      const data = Array.isArray(res.data) ? res.data : (res.data.content || []);
      // Apply strict scoping by employees' full names
      const nameSet = new Set(scopeNames);
      const scoped = data.filter((rec: any) => {
        const s = rec.senderName || '';
        const r = rec.recipientName || '';
        return nameSet.has(s) || nameSet.has(r);
      });
      setAllRecognitions(scoped);
    } catch (err: any) {
      setError('Failed to fetch recognitions from the backend. Please check if the backend is running and the endpoint is correct.');
      setAllRecognitions([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAllRecognitions();
    // eslint-disable-next-line
  }, [user, scopeNames]);

  useEffect(() => {
    if (allRecognitions.length > 0) {
      updateFrontendPage(page, pageSize);
    }
    // eslint-disable-next-line
  }, [sortField, sortOrder, page, pageSize, allRecognitions]);

  // Helper for frontend paging
  const updateFrontendPage = (newPage: number, newPageSize: number) => {
    const total = Math.max(1, Math.ceil(allRecognitions.length / newPageSize));
    setTotalPages(total);
    setPage(newPage);
    setPageSize(newPageSize);
  };

  // Page change handler
  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  // Page size change handler
  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPage(0);
    setPageSize(newSize);
  };

  const getUniqueValues = (field: string) => {
    return Array.from(new Set(allRecognitions.map(rec => rec[field] || '-'))).filter(v => v !== undefined && v !== null);
  };

  const handleHeaderClick = (col: any) => {
    if (filterableKeys.includes(col.key)) {
      setFilterField(col.key);
    } else if (["senderName", "recipientName", "rejectionReason", "message"].includes(col.key)) {
      // No sorting or filtering for these fields
      return;
    } else if (sortableArrowKeys.includes(col.key)) {
      // Arrow-based sorting for id, points, date
      if (sortField === col.key) {
        setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
      } else {
        setSortField(col.key);
        setSortOrder('asc');
      }
    } else {
      return;
    }
  };

  const getFilteredRecognitions = () => {
    let filtered = allRecognitions;
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      filtered = filtered.filter(rec =>
        [rec.id, rec.recognitionTypeName, rec.category, rec.level, rec.awardPoints, rec.senderName, rec.recipientName, rec.message, rec.approvalStatus, rec.rejectionReason]
          .map(val => (val !== undefined && val !== null ? String(val).toLowerCase() : ''))
          .some(val => val.includes(q))
      );
    }
    if (filterField && filterValue) {
      filtered = filtered.filter(rec => (rec[filterField] || '-') === filterValue);
    }
    return filtered;
  };

  // In getSortedRecognitions, handle sorting for id, awardPoints, sentAt
  const getSortedRecognitions = () => {
    return [...getFilteredRecognitions()].sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];
      if (sortField === 'sentAt') {
        aVal = a.sentAt || a.createdAt || 0;
        bVal = b.sentAt || b.createdAt || 0;
      }
      if (aVal === undefined || aVal === null) aVal = '';
      if (bVal === undefined || bVal === null) bVal = '';
      if (["id", "awardPoints", "sentAt"].includes(sortField)) {
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

  // Use getSortedRecognitions in getPagedRecognitions
  const getPagedRecognitions = () => {
    const sorted = getSortedRecognitions();
    const total = Math.max(1, Math.ceil(sorted.length / pageSize));
    if (totalPages !== total) setTotalPages(total);
    return sorted.slice(page * pageSize, (page + 1) * pageSize);
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
      // Build payload using current user as sender and recipient derived from ID
      const recipient = employeesList.find(e => String(e.id) === String(createForm.recipientId) || String(e.employeeId) === String(createForm.recipientId));
      const payload: any = {
        recognitionTypeName: createForm.recognitionTypeName,
        category: createForm.category,
        level: createForm.level,
        awardPoints: Number(createForm.awardPoints),
        senderEmail: user?.email,
        senderName: user ? (user.email.split('@')[0]) : undefined,
        recipientId: recipient?.id ?? recipient?.employeeId,
        recipientEmail: recipient?.email,
        recipientName: recipient ? [recipient.firstName, recipient.lastName].filter(Boolean).join(' ') : undefined,
        message: createForm.message,
      };
      await axios.post('/recognitions', payload);
      setCreateSuccess('Recognition created successfully');
      setCreateForm({ recognitionTypeName: '', category: '', level: '', awardPoints: '', recipientId: '', message: '' });
      // Reload recognitions
      setTimeout(() => window.location.reload(), 500); // quick reload for demo
    } catch (err: any) {
      setCreateError('Failed to create recognition');
    } finally {
      setCreating(false);
    }
  };

  const filterDropdownRef = useRef<HTMLDivElement | null>(null);

  // Only close dropdown on outside click, do not clear filter value
  useEffect(() => {
    if (!filterField) return;
    function handleClickOutside(event: MouseEvent) {
      if (
        filterDropdownRef.current &&
        !filterDropdownRef.current.contains(event.target as Node)
      ) {
        setFilterField(null); // Only close dropdown, do not clear filter value
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [filterField]);

  // Clear all filters button: only clears filterValue, not filterField
  const clearAllFilters = () => {
    setFilterValue(null);
    setFilterField(null);
  };

  // Recognition type metadata
  const [typeOptions, setTypeOptions] = useState<string[]>([]);
  const [categoryOptions, setCategoryOptions] = useState<string[]>([
    'Team Player',
    'Innovation',
    'Great Job',
    'Awesome Work',
    'Milestone Achieved',
  ]);
  const [levelOptions, setLevelOptions] = useState<string[]>([]);
  const [pointsOptions, setPointsOptions] = useState<number[]>([0, 5, 10, 25, 50, 100]);

  // Employees for recipient dropdown
  const [employeesList, setEmployeesList] = useState<any[]>([]);

  useEffect(() => {
    // Fetch recognition types metadata
    axios.get('/recognition-types?page=0&size=1000').then(res => {
      const data = Array.isArray(res.data) ? res.data : (res.data.content || res.data.items || []);
      // Map common possible keys: name, recognitionTypeName, type, typeName
      const rawTypes = data
        .map((d: any) => d?.name ?? d?.recognitionTypeName ?? d?.type ?? d?.typeName)
        .filter((v: any) => typeof v === 'string') as string[];
      const types: string[] = Array.from(new Set(rawTypes.map((t: string) => t.trim()).filter((t: string) => t.length > 0)));
      setTypeOptions(types.length > 0 ? types : ['award', 'ecard_with_points', 'ecard']);
      // levels will be fixed for 'award'
      setLevelOptions(['gold', 'silver', 'bronze']);
      // Points: derive common values if present
      const pts: number[] = Array.from(new Set(
        (data as any[])
          .map((d: any) => Number(d.awardPoints ?? d.points))
          .filter((n: any): n is number => typeof n === 'number' && !isNaN(n))
      ));
      if (pts.length > 0) setPointsOptions(pts.sort((a: number, b: number) => a - b));
     }).catch(() => {
       // Defaults if service fails
       setTypeOptions(['award', 'ecard_with_points', 'ecard']);
       setLevelOptions(['gold', 'silver', 'bronze']);
       setPointsOptions([0, 5, 10, 25, 50, 100]);
     });
    // Fetch employees for recipient dropdown
    axios.get('/employees?page=0&size=5000').then(res => {
      const list = Array.isArray(res.data) ? res.data : (res.data.content || res.data.items || []);
      setEmployeesList(list);
    }).catch(() => {
      setEmployeesList([]);
    });
    // Fetch more categories from recognitions backend
    axios.get('/recognitions?page=0&size=10000').then(res => {
      const data = Array.isArray(res.data) ? res.data : (res.data.content || res.data.items || []);
      const rawCats: string[] = data
        .map((r: any) => r.category)
        .filter((v: any): v is string => typeof v === 'string')
        .map((s: string) => s.trim())
        .filter((s: string) => s.length > 0);
      const cats: string[] = Array.from(new Set<string>(rawCats));
      if (cats.length > 0) {
        setCategoryOptions((prev: string[]) => {
          const merged: string[] = [...prev, ...cats];
          const uniq: string[] = Array.from(new Set<string>(merged));
          return uniq;
        });
      }
    }).catch(() => {
      // ignore; keep existing categories
    });
  }, []);

  // Dynamic behavior based on selected type and level
  const isAward = createForm.recognitionTypeName.toLowerCase() === 'award';
  const isEcard = createForm.recognitionTypeName.toLowerCase() === 'ecard';
  const isEcardWithPoints = createForm.recognitionTypeName.toLowerCase() === 'ecard_with_points';

  useEffect(() => {
    // When type changes, adjust available level/points and clear fields accordingly
    if (isAward) {
      // fixed levels
      setLevelOptions(['gold', 'silver', 'bronze']);
      // remove manual points dropdown by setting auto value on level
      if (createForm.level === 'gold') setCreateForm(prev => ({ ...prev, awardPoints: String(30) }));
      else if (createForm.level === 'silver') setCreateForm(prev => ({ ...prev, awardPoints: String(25) }));
      else if (createForm.level === 'bronze') setCreateForm(prev => ({ ...prev, awardPoints: String(20) }));
      else {
        // default to gold
        setCreateForm(prev => ({ ...prev, level: 'gold', awardPoints: String(30) }));
      }
      // pointsOptions not used for award; reset to empty to ensure dropdown hides values if condition changes
      setPointsOptions([]);
    } else if (isEcardWithPoints) {
      // No level; points only 5,10,15
      setCreateForm(prev => ({ ...prev, level: '', awardPoints: prev.awardPoints && ['5','10','15'].includes(prev.awardPoints) ? prev.awardPoints : '5' }));
      setPointsOptions([5, 10, 15]);
    } else if (isEcard) {
      // No level; no points
      setCreateForm(prev => ({ ...prev, level: '', awardPoints: '' }));
      setPointsOptions([]);
    } else {
      // Other types: clear level, use generic points options
      setCreateForm(prev => ({ ...prev, level: '', awardPoints: prev.awardPoints }));
      // restore generic options (ensure something present)
      setPointsOptions((opts) => (opts.length ? opts : [0, 5, 10, 25, 50, 100]));
    }
    // eslint-disable-next-line
  }, [createForm.recognitionTypeName]);

  // When level changes under 'award', auto-set points
  useEffect(() => {
    if (!isAward) return;
    if (createForm.level === 'gold') setCreateForm(prev => ({ ...prev, awardPoints: String(30) }));
    else if (createForm.level === 'silver') setCreateForm(prev => ({ ...prev, awardPoints: String(25) }));
    else if (createForm.level === 'bronze') setCreateForm(prev => ({ ...prev, awardPoints: String(20) }));
  }, [createForm.level, isAward]);

  return (
    <div style={{ width: '100%', minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 24, fontSize: '1.7rem', fontWeight: 600 }}>Recognition Management</h2>
      {error && <div style={{ color: 'red', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{error}</div>}
      {createError && <div style={{ color: 'red', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{createError}</div>}
      {createSuccess && <div style={{ color: 'green', marginBottom: 10, fontSize: '1rem', textAlign: 'center' }}>{createSuccess}</div>}
      <form onSubmit={handleCreate} style={{ width: '100%', display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20, alignItems: 'center', background: '#fff', padding: 10, borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0', fontSize: '0.7rem', justifyContent: 'center' }}>
        {/* Type dropdown */}
        <select name="recognitionTypeName" value={createForm.recognitionTypeName} onChange={handleCreateInputChange} required style={{ flex: '1 1 140px', fontSize: '0.7rem', padding: 6 }}>
          <option value="">Select Type</option>
          {typeOptions.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        {/* Category dropdown */}
        <select name="category" value={createForm.category} onChange={handleCreateInputChange} required style={{ flex: '1 1 140px', fontSize: '0.7rem', padding: 6 }}>
          <option value="">Select Category</option>
          {categoryOptions.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        {/* Level dropdown (only for award type) */}
        {isAward && (
          <select name="level" value={createForm.level} onChange={handleCreateInputChange} style={{ flex: '1 1 120px', fontSize: '0.7rem', padding: 6 }}>
            <option value="">Select Level</option>
            {levelOptions.map(l => <option key={l} value={l}>{l}</option>)}
          </select>
        )}
        {/* Points dropdown: award auto-set by level; ecard_with_points limited; ecard none */}
        {!isAward && !isEcard && (
          <select name="awardPoints" value={createForm.awardPoints} onChange={handleCreateInputChange} style={{ flex: '1 1 100px', fontSize: '0.7rem', padding: 6 }}>
            <option value="">Points</option>
            {pointsOptions.map(p => <option key={p} value={p}>{p}</option>)}
          </select>
        )}
        {/* Recipient ID dropdown */}
        <select name="recipientId" value={createForm.recipientId} onChange={handleCreateInputChange} required style={{ flex: '1 1 180px', fontSize: '0.7rem', padding: 6 }}>
          <option value="">Select Recipient</option>
          {employeesList.map(emp => (
            <option key={emp.id ?? emp.employeeId} value={emp.id ?? emp.employeeId}>
              {emp.firstName} {emp.lastName} ({emp.email})
            </option>
          ))}
        </select>
        {/* Message */}
        <input name="message" placeholder="Message" value={createForm.message} onChange={handleCreateInputChange} required style={{ flex: '2 1 160px', fontSize: '0.7rem', padding: 6 }} />
        <div style={{ flex: '1 1 100%', display: 'flex', justifyContent: 'center', gap: 10 }}>
          <button type="submit" disabled={creating} style={{ background: '#1976d2', color: '#fff', border: 'none', borderRadius: 6, padding: '7px 14px', fontSize: '0.7rem', cursor: 'pointer', fontWeight: 500 }}>Create</button>
        </div>
      </form>
      {showTable && (
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
      )}
      {/* Move search box below the page section */}
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 8, width: '100%' }}>
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search recognitions..."
          style={{ padding: '7px 12px', fontSize: '0.8rem', borderRadius: 6, border: '1px solid #ccc', width: 220 }}
        />
        {filterValue && (
          <button onClick={clearAllFilters} style={{ padding: '7px 14px', fontSize: '0.8rem', borderRadius: 6, background: '#f5f7fa', border: '1px solid #888', color: '#333', fontWeight: 500, cursor: 'pointer' }}>
            Clear Filters
          </button>
        )}
      </div>
      {showTable && (
      <table style={{ width: '100%', fontSize: '0.7rem', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e0e0' }}>
        <thead>
          <tr style={{ background: '#8da1bd' }}>
            {sortableColumns.map(col => (
              <th
                key={col.key}
                style={{ padding: 8, userSelect: 'none', whiteSpace: 'nowrap', position: 'relative', cursor: (filterableKeys.includes(col.key) ? 'pointer' : sortableArrowKeys.includes(col.key) ? 'pointer' : 'default') }}
                onClick={() => handleHeaderClick(col)}
              >
                {col.label}
                {/* Add a circle for box-based filtering fields */}
                {filterableKeys.includes(col.key) && (
                  <span style={{ color: '#000000', marginLeft: 6, fontSize: '1rem', verticalAlign: 'middle' }}>●</span>
                )}
                {/* Only show sort arrows for id, points, and date columns */}
                {sortableArrowKeys.includes(col.key) && (
                  <span style={{ marginLeft: 4, color: sortField === col.key ? '#bbb' : '#050505', fontWeight: 700, fontSize: '1rem', cursor: 'pointer' }}>
                    {sortField === col.key ? (sortOrder === 'asc' ? '▲' : '▼') : '▲▼'}
                  </span>
                )}
                {/* Dropdown for filterable columns except 'message' */}
                {filterField === col.key && filterableKeys.includes(col.key) && col.key !== 'message' && (
                  <div ref={filterDropdownRef} style={{ position: 'fixed', bottom: 24, left: 32, background: '#fff', border: '1px solid #ccc', borderRadius: 6, zIndex: 1000, minWidth: 180, boxShadow: '0 2px 16px #b0b0b0', padding: 12 }}>
                    <div style={{ marginBottom: 4, fontWeight: 600 }}>Filter {col.label}</div>
                    <div style={{ maxHeight: 220, overflowY: 'auto' }}>
                      {getUniqueValues(col.key).map(val => (
                        <div key={val} style={{ padding: '6px 12px', cursor: 'pointer', background: filterValue === val ? '#e0e0e0' : '#fff', borderRadius: 4, marginBottom: 2 }}
                          onClick={() => { setFilterValue(val); setFilterField(null); }}>
                          {val}
                        </div>
                      ))}
                    </div>
                    <div style={{ marginTop: 6 }}>
                      <button onClick={clearAllFilters} style={{ fontSize: '0.7rem', padding: '3px 8px', borderRadius: 4, border: '1px solid #888', background: '#f5f7fa', cursor: 'pointer' }}>Clear Filters</button>
                    </div>
                  </div>
                )}
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
      )}
      {showTable && !loading && !error && getPagedRecognitions().length === 0 && (
        <div style={{ fontSize: '0.7rem', color: '#888', marginTop: 12 }}>No recognitions found.</div>
      )}
    </div>
  );
};

export default RecognitionManagement;
