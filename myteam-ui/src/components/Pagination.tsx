import React from 'react';

interface PaginationProps {
  page: number;
  pageSize: number;
  totalPages: number;
  onPageChange: (page: number, pageSize: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({ page, pageSize, totalPages, onPageChange }) => (
  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 16, gap: 12 }}>
    <button style={{ fontSize: '0.7rem', padding: '4px 10px' }} disabled={page === 0} onClick={() => onPageChange(0, pageSize)}>First</button>
    <button style={{ fontSize: '0.7rem', padding: '4px 10px' }} disabled={page === 0} onClick={() => onPageChange(page - 1, pageSize)}>Prev</button>
    {Array.from({ length: totalPages }, (_, i) => i).map(i => {
      if (totalPages > 7 && Math.abs(i - page) > 2 && i !== 0 && i !== totalPages - 1) return null;
      return (
        <button
          key={i}
          style={{ fontSize: '0.7rem', padding: '2px 8px', margin: '0 2px', background: i === page ? '#8da1bd' : '#fff', border: i === page ? '1px solid #8da1bd' : '1px solid #ccc', borderRadius: 4 }}
          disabled={i === page}
          onClick={() => onPageChange(i, pageSize)}
        >
          {i + 1}
        </button>
      );
    })}
    <button style={{ fontSize: '0.7rem', padding: '4px 10px' }} disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1, pageSize)}>Next</button>
    <button style={{ fontSize: '0.7rem', padding: '4px 10px' }} disabled={page + 1 >= totalPages} onClick={() => onPageChange(totalPages - 1, pageSize)}>Last</button>
    <span style={{ fontSize: '0.7rem', marginLeft: 16 }}>Page size:</span>
    <input
      type="number"
      min={1}
      max={100}
      value={pageSize}
      onChange={e => {
        let val = Number(e.target.value);
        if (val < 1) val = 1;
        if (val > 100) val = 100;
        onPageChange(0, val);
      }}
      style={{ width: 60, fontSize: '0.7rem', textAlign: 'center' }}
    />
  </div>
);

export default Pagination;

