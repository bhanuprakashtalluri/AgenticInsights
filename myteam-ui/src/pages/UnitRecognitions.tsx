import React from 'react';
import { useAuth } from '../services/auth';
import Sidebar from '../components/Sidebar';

const cardStyle = {
  background: '#fff',
  borderRadius: 10,
  boxShadow: '0 2px 8px #e0e0e0',
  padding: 24,
  marginBottom: 32,
  maxWidth: 900,
  marginLeft: 'auto',
  marginRight: 'auto',
};

const tableStyle = {
  width: '100%',
  fontSize: '0.95rem',
  background: '#f9f9fb',
  borderRadius: 8,
  marginBottom: 24,
  borderCollapse: 'separate' as const,
  borderSpacing: 0,
};

const thStyle = {
  background: '#8da1bd',
  color: '#fff',
  padding: 10,
  textAlign: 'left' as const,
  fontWeight: 600,
  borderTopLeftRadius: 8,
  borderTopRightRadius: 8,
};

const tdStyle = {
  padding: 10,
  borderBottom: '1px solid #e0e0e0',
};

const UnitRecognitions: React.FC = () => {
  const { user } = useAuth();
  const [unitRecs, setUnitRecs] = React.useState<any[]>([]);

  React.useEffect(() => {
    if (!user) return;
    fetch(`/recognitions?page=0&size=10000`)
      .then(res => res.json())
      .then(dataRaw => {
        const data = Array.isArray(dataRaw) ? dataRaw : (dataRaw.content || []);
        setUnitRecs(data.filter((rec: any) => rec.unitId && user.unitId && rec.unitId === user.unitId));
      });
  }, [user]);

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f5f7fa' }}>
      <div style={{ width: 180, minWidth: 180, background: '#fff', boxShadow: '2px 0 8px #e0e0e0' }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: 32 }}>
        <div style={cardStyle}>
          <h1 style={{ textAlign: 'center', marginBottom: 32 }}>Unit Recognitions</h1>
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>Sender</th>
                <th style={thStyle}>Recipient</th>
                <th style={thStyle}>Message</th>
                <th style={thStyle}>Points</th>
                <th style={thStyle}>Date</th>
              </tr>
            </thead>
            <tbody>
              {unitRecs.length === 0 ? <tr><td colSpan={5} style={tdStyle}>No unit recognitions found.</td></tr> : unitRecs.map((rec, i) => (
                <tr key={i}>
                  <td style={tdStyle}>{rec.senderName || '-'}</td>
                  <td style={tdStyle}>{rec.recipientName || '-'}</td>
                  <td style={tdStyle}>{rec.message || '-'}</td>
                  <td style={tdStyle}>{rec.awardPoints || '-'}</td>
                  <td style={tdStyle}>{rec.sentAt ? new Date(rec.sentAt * 1000).toLocaleDateString() : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default UnitRecognitions;
