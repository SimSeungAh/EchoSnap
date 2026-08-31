import {
  type FormEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useState,
} from 'react';
import {
  Navigate,
  NavLink,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import { adminApi, authApi } from './api';
import { CONFIG } from './config';
import type {
  AiCorrection, CollectionArea, Dashboard, Guide, Notification,
  Residence, ReviewStatus, Schedule, SyncLog, User, UserStatus, WasteItem
} from './types';

const icons: Record<string, string> = {
  dashboard: '▦',
  users: '♙',
  residence: '⌂',
  area: '⌖',
  schedule: '◷',
  waste: '♻',
  guide: '✓',
  ai: '✦',
  data: '⇄',
  notification: '◉',
  settings: '⚙',
};

function Badge({ value }: { value: string }) {
  const key = value.toUpperCase();
  const good = ['ACTIVE', 'APPROVED', 'SUCCESS', 'SENT'].includes(key);
  const warn = ['PENDING', 'RUNNING', 'SCHEDULED', 'DRAFT'].includes(key);
  const bad = ['REJECTED', 'FAILED', 'SUSPENDED'].includes(key);
  const labels: Record<string, string> = {
    ACTIVE: '활성', INACTIVE: '비활성', SUSPENDED: '정지',
    APPROVED: '승인', PENDING: '대기', REJECTED: '거절',
    SUCCESS: '성공', FAILED: '실패', RUNNING: '진행 중',
    SENT: '발송 완료', SCHEDULED: '예약', DRAFT: '임시저장',
    USER: '사용자', ADMIN: '관리자',
  };
  return <span className={`badge ${good ? 'good' : warn ? 'warn' : bad ? 'bad' : ''}`}>{labels[key] ?? value}</span>;
}

function PageHeader({ title, subtitle, action }: { title: string; subtitle: string; action?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <div className="eyebrow">SMARTRECYCLE ADMIN</div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      {action && <div className="page-actions">{action}</div>}
    </div>
  );
}

function Modal({ open, title, children, onClose, footer }: { open: boolean; title: string; children: ReactNode; onClose: () => void; footer?: ReactNode }) {
  if (!open) return null;
  return (
    <div className="modal-bg" onMouseDown={e => e.currentTarget === e.target && onClose()}>
      <div className="modal">
        <div className="modal-head">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

function Loading() {
  return <div className="state-card"><span className="spinner" /> 데이터를 불러오고 있어요.</div>;
}

function Empty({ text }: { text: string }) {
  return <div className="state-card muted">{text}</div>;
}

function TableWrap({ children }: { children: ReactNode }) {
  return <div className="table-wrap"><div className="table-scroll">{children}</div></div>;
}

type Session = { email: string; role: string } | null;

export default function App() {
  const [session, setSession] = useState<Session>(() => authApi.session());

  useEffect(() => {
    const expired = () => setSession(null);
    window.addEventListener('smartrecycle-auth-expired', expired);
    return () => window.removeEventListener('smartrecycle-auth-expired', expired);
  }, []);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage session={session} onLogin={setSession} />} />
      <Route path="/*" element={session ? <Shell session={session} onLogout={() => { authApi.logout(); setSession(null); }} /> : <Navigate to="/login" replace />} />
    </Routes>
  );
}

function LoginPage({ session, onLogin }: { session: Session; onLogin: (session: NonNullable<Session>) => void }) {
  const navigate = useNavigate();
  const [email, setEmail] = useState(CONFIG.useMocks ? 'admin@smartrecycle.com' : '');
  const [password, setPassword] = useState(CONFIG.useMocks ? 'Admin1234!' : '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  if (session) return <Navigate to="/dashboard" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const next = await authApi.login(email.trim(), password);
      onLogin(next);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <section className="login-hero">
        <div className="login-mark">♻</div>
        <div className="eyebrow light">SMARTRECYCLE</div>
        <h1>더 정확한 분리배출을<br />위한 운영 콘솔</h1>
        <p>사용자, 거주지, 배출 일정, 폐기물 가이드와 AI 정정 데이터까지 한 곳에서 관리합니다.</p>
        <div className="hero-tags"><span>관리자 전용</span><span>JWT 인증</span><span>AI 검수</span></div>
      </section>
      <section className="login-side">
        <form className="login-card" onSubmit={submit}>
          <div className="eyebrow">ADMIN LOGIN</div>
          <h2>관리자 로그인</h2>
          <p>운영 계정으로 로그인해주세요.</p>
          {CONFIG.useMocks && <div className="demo">데모 계정<br /><b>admin@smartrecycle.com</b><br />Admin1234!</div>}
          {error && <div className="error-box">{error}</div>}
          <label><span>이메일</span><input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label>
          <label><span>비밀번호</span><input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
          <button className="btn primary login-btn" disabled={busy}>{busy ? '로그인 중...' : '관리자 로그인 →'}</button>
        </form>
      </section>
    </div>
  );
}

const navGroups = [
  ['개요', [['/dashboard', 'dashboard', '대시보드']]],
  ['사용자·거주지', [['/users', 'users', '사용자 관리'], ['/residences', 'residence', '거주지 관리'], ['/collection-areas', 'area', '수거구역 관리'], ['/schedules', 'schedule', '배출 일정 관리']]],
  ['분리배출 정보', [['/waste-items', 'waste', '폐기물 품목'], ['/guides', 'guide', '분리배출 가이드']]],
  ['AI·운영', [['/ai-corrections', 'ai', 'AI 정정 검수'], ['/public-data', 'data', '공공데이터'], ['/notifications', 'notification', '알림 관리']]],
] as const;

function Shell({ session, onLogout }: { session: NonNullable<Session>; onLogout: () => void }) {
  const [mobile, setMobile] = useState(false);
  const location = useLocation();

  return (
    <div className="layout">
      <aside className={`sidebar ${mobile ? 'open' : ''}`}>
        <div className="brand"><span>♻</span><div><b>SmartRecycle</b><small>ADMIN CONSOLE</small></div></div>
        <nav>
          {navGroups.map(([group, items]) => (
            <div className="nav-group" key={group}>
              <div className="nav-label">{group}</div>
              {items.map(([to, icon, label]) => (
                <NavLink key={to} to={to} onClick={() => setMobile(false)} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                  <span className="nav-icon">{icons[icon]}</span>{label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <NavLink to="/settings" className="nav-item"><span className="nav-icon">{icons.settings}</span>관리자 설정</NavLink>
          <button className="nav-item logout" onClick={onLogout}><span className="nav-icon">↪</span>로그아웃</button>
        </div>
      </aside>
      {mobile && <button className="backdrop" onClick={() => setMobile(false)} />}
      <main>
        <header className="topbar">
          <div className="top-left"><button className="mobile-menu" onClick={() => setMobile(true)}>☰</button><div><b>{routeTitle(location.pathname)}</b><small>SmartRecycle 운영 관리</small></div></div>
          <div className="top-right">{CONFIG.useMocks && <span className="demo-pill">DEMO MODE</span>}<div className="avatar">관</div><div className="profile"><b>관리자</b><small>{session.email}</small></div></div>
        </header>
        <div className="content">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/residences" element={<ResidencesPage />} />
            <Route path="/collection-areas" element={<AreasPage />} />
            <Route path="/schedules" element={<SchedulesPage />} />
            <Route path="/waste-items" element={<WasteItemsPage />} />
            <Route path="/guides" element={<GuidesPage />} />
            <Route path="/ai-corrections" element={<AiPage />} />
            <Route path="/public-data" element={<PublicDataPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
      </main>
    </div>
  );
}

function routeTitle(path: string) {
  const map: Record<string, string> = {
    '/dashboard': '대시보드', '/users': '사용자 관리', '/residences': '거주지 관리',
    '/collection-areas': '수거구역 관리', '/schedules': '배출 일정 관리',
    '/waste-items': '폐기물 품목 관리', '/guides': '분리배출 가이드 관리',
    '/ai-corrections': 'AI 정정 검수', '/public-data': '공공데이터 관리',
    '/notifications': '알림 관리', '/settings': '관리자 설정',
  };
  return map[path] ?? 'SmartRecycle';
}

function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  useEffect(() => { adminApi.dashboard().then(setData); }, []);
  if (!data) return <Loading />;

  const stats = [
    ['전체 사용자', data.users, `활성 ${data.activeUsers}명`, 'users'],
    ['승인 대기 거주지', data.pendingResidences, '관리자 확인 필요', 'residence'],
    ['등록 폐기물 품목', data.wasteItems, '검색·가이드 대상', 'waste'],
    ['AI 정정 검수 대기', data.pendingAi, '재학습 후보 검수', 'ai'],
    ['예약 알림', data.scheduledNotifications, '발송 스케줄', 'notification'],
  ];

  return <>
    <PageHeader title="운영 현황" subtitle="SmartRecycle 서비스의 핵심 상태를 빠르게 확인합니다." />
    <div className="stats">{stats.map(([label, value, helper, icon]) => <article className="stat" key={String(label)}><div><span>{label}</span><i>{icons[String(icon)]}</i></div><strong>{value}</strong><small>{helper}</small></article>)}</div>
    <div className="dash-grid">
      <section className="panel"><h2>관리자 우선 처리 항목</h2><div className="todo-list"><div><b>AI 정정 검수</b><span>{data.pendingAi}건 대기</span></div><div><b>거주지 승인</b><span>{data.pendingResidences}건 대기</span></div><div><b>예약 알림</b><span>{data.scheduledNotifications}건</span></div></div></section>
      <section className="panel"><h2>현재 운영 구조</h2><div className="flow"><span>공공데이터</span><b>→</b><span>수거구역·일정</span><b>→</b><span>사용자 맞춤 안내</span></div><div className="flow"><span>AI 추정</span><b>→</b><span>사용자 정정</span><b>→</b><span>관리자 검수</span></div></section>
    </div>
  </>;
}

function UsersPage() {
  const [items, setItems] = useState<User[]>([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<User | null>(null);
  const load = async () => { setLoading(true); setItems(await adminApi.users(q)); setLoading(false); };
  useEffect(() => { load(); }, []);

  async function status(user: User, value: UserStatus) { await adminApi.setUserStatus(user.id, value); await load(); setSelected(s => s ? { ...s, status: value } : s); }
  async function role(user: User, value: 'USER' | 'ADMIN') { await adminApi.setUserRole(user.id, value); await load(); setSelected(s => s ? { ...s, role: value } : s); }

  return <>
    <PageHeader title="사용자 관리" subtitle="계정 상태, 역할, 거주지 연결 정보를 관리합니다." />
    <div className="toolbar"><input className="search" placeholder="이메일, 이름, 주소 검색" value={q} onChange={e => setQ(e.target.value)} onKeyDown={e => e.key === 'Enter' && load()} /><button className="btn primary" onClick={load}>검색</button></div>
    {loading ? <Loading /> : !items.length ? <Empty text="검색 결과가 없습니다." /> :
      <TableWrap><table><thead><tr><th>사용자</th><th>역할</th><th>상태</th><th>거주지</th><th>주소</th><th>가입일</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.name}</b><small>{x.email}</small></td><td><Badge value={x.role} /></td><td><Badge value={x.status} /></td><td>{x.residence}</td><td>{x.address}</td><td>{x.createdAt}</td><td className="right"><button className="btn small" onClick={() => setSelected(x)}>관리</button></td></tr>)}</tbody></table></TableWrap>}
    <Modal open={!!selected} title="사용자 상세 관리" onClose={() => setSelected(null)} footer={<button className="btn" onClick={() => setSelected(null)}>닫기</button>}>
      {selected && <div className="form-grid"><div className="info"><span>이름</span><b>{selected.name}</b></div><div className="info"><span>이메일</span><b>{selected.email}</b></div><label><span>계정 상태</span><select value={selected.status} onChange={e => status(selected, e.target.value as UserStatus)}><option value="ACTIVE">활성</option><option value="INACTIVE">비활성</option><option value="SUSPENDED">정지</option></select></label><label><span>역할</span><select value={selected.role} onChange={e => role(selected, e.target.value as 'USER' | 'ADMIN')}><option value="USER">사용자</option><option value="ADMIN">관리자</option></select></label><div className="info full"><span>거주지</span><b>{selected.residence} · {selected.address}</b></div></div>}
    </Modal>
  </>;
}

function ResidencesPage() {
  const [items, setItems] = useState<Residence[]>([]);
  const [loading, setLoading] = useState(true);
  const load = async () => { setLoading(true); setItems(await adminApi.residences()); setLoading(false); };
  useEffect(() => { load(); }, []);
  async function review(id: number, ok: boolean) { await adminApi.reviewResidence(id, ok); await load(); }
  return <>
    <PageHeader title="거주지 관리" subtitle="공동주택 등록 승인과 일반주택 수거구역 연결 상태를 확인합니다." />
    {loading ? <Loading /> : <TableWrap><table><thead><tr><th>거주지</th><th>유형</th><th>주소</th><th>건물관리번호</th><th>수거구역</th><th>상태</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.name}</b></td><td>{x.type}</td><td>{x.address}</td><td>{x.buildingNo ?? '-'}</td><td>{x.area ?? '미연결'}</td><td><Badge value={x.approval} /></td><td className="right">{x.approval === 'PENDING' && <div className="actions"><button className="btn small primary" onClick={() => review(x.id, true)}>승인</button><button className="btn small danger" onClick={() => review(x.id, false)}>거절</button></div>}</td></tr>)}</tbody></table></TableWrap>}
  </>;
}

function AreasPage() {
  const [items, setItems] = useState<CollectionArea[]>([]);
  const [editing, setEditing] = useState<CollectionArea | null>(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState(''); const [district, setDistrict] = useState('부산진구'); const [dongs, setDongs] = useState('');
  const load = async () => setItems(await adminApi.areas());
  useEffect(() => { load(); }, []);
  function open(item?: CollectionArea) { if (item) { setEditing(item); setName(item.name); setDistrict(item.district); setDongs(item.dongs.join(', ')); } else { setEditing(null); setName(''); setDistrict('부산진구'); setDongs(''); } setCreating(true); }
  async function save() { await adminApi.saveArea({ id: editing?.id, name, district, dongs: dongs.split(',').map(x => x.trim()).filter(Boolean), active: true }); setCreating(false); await load(); }
  return <>
    <PageHeader title="수거구역 관리" subtitle="행정동·주소 기반 일반주택 수거구역을 관리합니다." action={<button className="btn primary" onClick={() => open()}>+ 수거구역 추가</button>} />
    <TableWrap><table><thead><tr><th>구역명</th><th>자치구</th><th>행정동</th><th>상태</th><th>최근 갱신</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.name}</b></td><td>{x.district}</td><td>{x.dongs.join(', ')}</td><td><Badge value={x.active ? 'ACTIVE' : 'INACTIVE'} /></td><td>{x.updatedAt}</td><td className="right"><button className="btn small" onClick={() => open(x)}>수정</button></td></tr>)}</tbody></table></TableWrap>
    <Modal open={creating} title={editing ? '수거구역 수정' : '수거구역 추가'} onClose={() => setCreating(false)} footer={<><button className="btn" onClick={() => setCreating(false)}>취소</button><button className="btn primary" onClick={save} disabled={!name.trim()}>저장</button></>}>
      <div className="form-grid"><label><span>구역명</span><input value={name} onChange={e => setName(e.target.value)} /></label><label><span>자치구</span><input value={district} onChange={e => setDistrict(e.target.value)} /></label><label className="full"><span>행정동 (쉼표 구분)</span><input value={dongs} onChange={e => setDongs(e.target.value)} placeholder="부전1동, 부전2동" /></label></div>
    </Modal>
  </>;
}

function SchedulesPage() {
  const [items, setItems] = useState<Schedule[]>([]);
  const [editing, setEditing] = useState<Schedule | null>(null);
  const [open, setOpen] = useState(false);
  const [area, setArea] = useState(''); const [category, setCategory] = useState(''); const [day, setDay] = useState('월요일'); const [time, setTime] = useState('18:00 ~ 21:00'); const [note, setNote] = useState('');
  const load = async () => setItems(await adminApi.schedules());
  useEffect(() => { load(); }, []);
  function show(x?: Schedule) { setEditing(x ?? null); setArea(x?.area ?? '부전1동 A구역'); setCategory(x?.category ?? '종이류'); setDay(x?.day ?? '월요일'); setTime(x?.time ?? '18:00 ~ 21:00'); setNote(x?.note ?? ''); setOpen(true); }
  async function save() { await adminApi.saveSchedule({ id: editing?.id, area, category, day, time, note, active: true }); setOpen(false); await load(); }
  return <>
    <PageHeader title="배출 일정 관리" subtitle="수거구역별 품목 배출 요일과 시간대를 관리합니다." action={<button className="btn primary" onClick={() => show()}>+ 일정 추가</button>} />
    <TableWrap><table><thead><tr><th>수거구역</th><th>품목</th><th>배출일</th><th>시간</th><th>비고</th><th>상태</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.area}</b></td><td>{x.category}</td><td>{x.day}</td><td>{x.time}</td><td>{x.note || '-'}</td><td><Badge value={x.active ? 'ACTIVE' : 'INACTIVE'} /></td><td className="right"><button className="btn small" onClick={() => show(x)}>수정</button></td></tr>)}</tbody></table></TableWrap>
    <Modal open={open} title={editing ? '배출 일정 수정' : '배출 일정 추가'} onClose={() => setOpen(false)} footer={<><button className="btn" onClick={() => setOpen(false)}>취소</button><button className="btn primary" onClick={save}>저장</button></>}>
      <div className="form-grid"><label><span>수거구역</span><input value={area} onChange={e => setArea(e.target.value)} /></label><label><span>품목 구분</span><input value={category} onChange={e => setCategory(e.target.value)} /></label><label><span>요일</span><input value={day} onChange={e => setDay(e.target.value)} /></label><label><span>시간</span><input value={time} onChange={e => setTime(e.target.value)} /></label><label className="full"><span>비고</span><input value={note} onChange={e => setNote(e.target.value)} /></label></div>
    </Modal>
  </>;
}

function WasteItemsPage() {
  const [items, setItems] = useState<WasteItem[]>([]);
  const [q, setQ] = useState('');
  const [editing, setEditing] = useState<WasteItem | null>(null);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState(''); const [category, setCategory] = useState('폐전지·전자제품'); const [aliases, setAliases] = useState(''); const [ai, setAi] = useState(false);
  const load = async () => setItems(await adminApi.wasteItems(q));
  useEffect(() => { load(); }, []);
  function show(x?: WasteItem) { setEditing(x ?? null); setName(x?.name ?? ''); setCategory(x?.category ?? '폐전지·전자제품'); setAliases(x?.aliases.join(', ') ?? ''); setAi(x?.aiSupported ?? false); setOpen(true); }
  async function save() { await adminApi.saveWasteItem({ id: editing?.id, name, category, aliases: aliases.split(',').map(x => x.trim()).filter(Boolean), aiSupported: ai, active: true }); setOpen(false); await load(); }
  return <>
    <PageHeader title="폐기물 품목 관리" subtitle="AI 인식 범위와 별개로 사용자가 검색할 수 있는 전체 폐기물 품목을 관리합니다." action={<button className="btn primary" onClick={() => show()}>+ 품목 추가</button>} />
    <div className="notice"><b>AI 미지원 품목도 서비스에는 등록할 수 있어요.</b><span>폐건전지, 전선, 마우스, 충전기, 키보드 등은 직접 검색 fallback으로 정확한 가이드를 제공할 수 있습니다.</span></div>
    <div className="toolbar"><input className="search" value={q} onChange={e => setQ(e.target.value)} placeholder="품목명, 카테고리, 별칭 검색" /><button className="btn primary" onClick={load}>검색</button></div>
    <TableWrap><table><thead><tr><th>품목</th><th>카테고리</th><th>검색 별칭</th><th>AI 인식</th><th>상태</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.name}</b></td><td>{x.category}</td><td>{x.aliases.join(', ') || '-'}</td><td><span className={x.aiSupported ? 'ai-ok' : 'muted'}>{x.aiSupported ? '지원' : '검색 전용'}</span></td><td><Badge value={x.active ? 'ACTIVE' : 'INACTIVE'} /></td><td className="right"><button className="btn small" onClick={() => show(x)}>수정</button></td></tr>)}</tbody></table></TableWrap>
    <Modal open={open} title={editing ? '폐기물 품목 수정' : '폐기물 품목 추가'} onClose={() => setOpen(false)} footer={<><button className="btn" onClick={() => setOpen(false)}>취소</button><button className="btn primary" onClick={save} disabled={!name.trim()}>저장</button></>}>
      <div className="form-grid"><label><span>품목명</span><input value={name} onChange={e => setName(e.target.value)} /></label><label><span>카테고리</span><input value={category} onChange={e => setCategory(e.target.value)} /></label><label className="full"><span>검색 별칭</span><input value={aliases} onChange={e => setAliases(e.target.value)} placeholder="건전지, 배터리, AA배터리" /></label><label className="check full"><input type="checkbox" checked={ai} onChange={e => setAi(e.target.checked)} /> 현재 AI 모델 지원 품목</label></div>
    </Modal>
  </>;
}

function GuidesPage() {
  const [items, setItems] = useState<Guide[]>([]);
  const [editing, setEditing] = useState<Guide | null>(null);
  const [open, setOpen] = useState(false);
  const [wasteItem, setWasteItem] = useState(''); const [summary, setSummary] = useState(''); const [method, setMethod] = useState(''); const [caution, setCaution] = useState(''); const [checks, setChecks] = useState('');
  const load = async () => setItems(await adminApi.guides());
  useEffect(() => { load(); }, []);
  function show(x?: Guide) { setEditing(x ?? null); setWasteItem(x?.wasteItem ?? ''); setSummary(x?.summary ?? ''); setMethod(x?.method ?? ''); setCaution(x?.caution ?? ''); setChecks(x?.checks.join('\n') ?? ''); setOpen(true); }
  async function save() { await adminApi.saveGuide({ id: editing?.id, wasteItem, summary, method, caution, checks: checks.split('\n').map(x => x.trim()).filter(Boolean) }); setOpen(false); await load(); }
  return <>
    <PageHeader title="분리배출 가이드 관리" subtitle="품목별 배출 방법, 주의사항, 사용자 체크리스트를 관리합니다." action={<button className="btn primary" onClick={() => show()}>+ 가이드 추가</button>} />
    <div className="guide-list">{items.map(x => <article className="guide" key={x.id}><div className="guide-head"><div><small>{x.wasteItem}</small><h3>{x.summary}</h3></div><button className="btn small" onClick={() => show(x)}>수정</button></div><div className="guide-cols"><div><b>배출 방법</b><p>{x.method}</p></div><div><b>주의사항</b><p>{x.caution}</p></div></div><div className="chips">{x.checks.map(c => <span key={c}>{c}</span>)}</div></article>)}</div>
    <Modal open={open} title={editing ? '분리배출 가이드 수정' : '분리배출 가이드 추가'} onClose={() => setOpen(false)} footer={<><button className="btn" onClick={() => setOpen(false)}>취소</button><button className="btn primary" onClick={save} disabled={!wasteItem.trim() || !summary.trim()}>저장</button></>}>
      <div className="form-grid"><label><span>품목</span><input value={wasteItem} onChange={e => setWasteItem(e.target.value)} /></label><label><span>요약</span><input value={summary} onChange={e => setSummary(e.target.value)} /></label><label className="full"><span>배출 방법</span><textarea value={method} onChange={e => setMethod(e.target.value)} rows={4} /></label><label className="full"><span>주의사항</span><textarea value={caution} onChange={e => setCaution(e.target.value)} rows={3} /></label><label className="full"><span>체크리스트 (한 줄에 하나)</span><textarea value={checks} onChange={e => setChecks(e.target.value)} rows={5} /></label></div>
    </Modal>
  </>;
}

function AiPage() {
  const [items, setItems] = useState<AiCorrection[]>([]);
  const [filter, setFilter] = useState<ReviewStatus | 'ALL'>('PENDING');
  const [selected, setSelected] = useState<AiCorrection | null>(null);
  const [memo, setMemo] = useState('');
  const load = async () => setItems(await adminApi.corrections(filter));
  useEffect(() => { load(); }, [filter]);
  async function review(ok: boolean) { if (!selected) return; await adminApi.reviewCorrection(selected.id, ok, memo); setSelected(null); setMemo(''); await load(); }

  return <>
    <PageHeader title="AI 사용자 정정 검수" subtitle="AI 원본 예측과 사용자 수정값을 비교하고 재학습 후보를 승인합니다." />
    <div className="notice"><b>사용자 정정값을 바로 학습하지 않습니다.</b><span>PENDING → 관리자 검수 → APPROVED / REJECTED. 승인된 데이터만 향후 재학습 후보로 사용합니다.</span></div>
    <div className="tabs">{(['PENDING', 'APPROVED', 'REJECTED', 'ALL'] as const).map(x => <button className={filter === x ? 'active' : ''} onClick={() => setFilter(x)} key={x}>{x === 'PENDING' ? '검수 대기' : x === 'APPROVED' ? '승인' : x === 'REJECTED' ? '거절' : '전체'}</button>)}</div>
    {!items.length ? <Empty text="현재 필터에 해당하는 정정 데이터가 없습니다." /> : <TableWrap><table><thead><tr><th>ImageLog</th><th>사용자</th><th>AI 원본</th><th>신뢰도</th><th>사용자 정정</th><th>상태</th><th>정정 시각</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td>#{x.imageLogId}</td><td>{x.userEmail}</td><td><b>{x.aiItem}</b></td><td>{(x.aiConfidence * 100).toFixed(1)}%</td><td><b className="ai-ok">{x.correctedItem}</b></td><td><Badge value={x.status} /></td><td>{x.correctedAt}</td><td className="right"><button className="btn small" onClick={() => { setSelected(x); setMemo(x.memo ?? ''); }}>{x.status === 'PENDING' ? '검수하기' : '상세'}</button></td></tr>)}</tbody></table></TableWrap>}
    <Modal open={!!selected} title={`AI 정정 검수 · ImageLog #${selected?.imageLogId ?? ''}`} onClose={() => setSelected(null)} footer={selected?.status === 'PENDING' ? <><button className="btn danger" onClick={() => review(false)}>거절</button><button className="btn primary" onClick={() => review(true)}>승인</button></> : <button className="btn" onClick={() => setSelected(null)}>닫기</button>}>
      {selected && <><div className="compare"><div><span>AI 원본</span><b>{selected.aiItem}</b><small>{(selected.aiConfidence * 100).toFixed(1)}%</small></div><strong>→</strong><div className="correct"><span>사용자 정정</span><b>{selected.correctedItem}</b><small>관리자 확인 필요</small></div></div><label><span>검수 메모</span><textarea rows={4} disabled={selected.status !== 'PENDING'} value={memo} onChange={e => setMemo(e.target.value)} placeholder="승인/거절 사유 또는 재학습 참고사항" /></label></>}
    </Modal>
  </>;
}

function PublicDataPage() {
  const [items, setItems] = useState<SyncLog[]>([]);
  const [syncing, setSyncing] = useState(false);
  const load = async () => setItems(await adminApi.syncLogs());
  useEffect(() => { load(); }, []);
  async function sync() { setSyncing(true); await adminApi.syncPublicData(); await load(); setSyncing(false); }
  return <>
    <PageHeader title="공공데이터 동기화" subtitle="생활폐기물 배출정보와 수거구역 데이터를 수동 동기화하고 결과를 확인합니다." action={<button className="btn primary" onClick={sync} disabled={syncing}>{syncing ? '동기화 중...' : '↻ 지금 동기화'}</button>} />
    <div className="notice"><b>공공데이터 → SmartRecycle 내부 도메인 변환</b><span>외부 원문을 그대로 노출하지 않고 수거구역, 일정, 품목 체계와 매핑해 사용자에게 제공합니다.</span></div>
    <TableWrap><table><thead><tr><th>데이터 소스</th><th>상태</th><th>추가</th><th>갱신</th><th>실패</th><th>실행 시각</th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.source}</b></td><td><Badge value={x.status} /></td><td>{x.inserted}</td><td>{x.updated}</td><td>{x.failed}</td><td>{x.at}</td></tr>)}</tbody></table></TableWrap>
  </>;
}

function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([]);
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState(''); const [body, setBody] = useState(''); const [target, setTarget] = useState('전체 사용자'); const [at, setAt] = useState('');
  const load = async () => setItems(await adminApi.notifications());
  useEffect(() => { load(); }, []);
  async function save() { await adminApi.createNotification({ title, body, target, at: at || undefined }); setOpen(false); setTitle(''); setBody(''); setAt(''); await load(); }
  async function send(id: number) { await adminApi.sendNotification(id); await load(); }
  return <>
    <PageHeader title="알림 관리" subtitle="거주지·수거구역별 배출 일정 안내와 운영 공지를 관리합니다." action={<button className="btn primary" onClick={() => setOpen(true)}>+ 알림 작성</button>} />
    <TableWrap><table><thead><tr><th>제목</th><th>대상</th><th>상태</th><th>예약/발송 시각</th><th></th></tr></thead><tbody>{items.map(x => <tr key={x.id}><td><b>{x.title}</b><small>{x.body}</small></td><td>{x.target}</td><td><Badge value={x.status} /></td><td>{x.at ?? '-'}</td><td className="right">{x.status !== 'SENT' && <button className="btn small" onClick={() => send(x.id)}>즉시 발송</button>}</td></tr>)}</tbody></table></TableWrap>
    <Modal open={open} title="알림 작성" onClose={() => setOpen(false)} footer={<><button className="btn" onClick={() => setOpen(false)}>취소</button><button className="btn primary" onClick={save} disabled={!title.trim() || !body.trim()}>저장</button></>}>
      <div className="form-grid"><label className="full"><span>제목</span><input value={title} onChange={e => setTitle(e.target.value)} /></label><label className="full"><span>내용</span><textarea rows={5} value={body} onChange={e => setBody(e.target.value)} /></label><label><span>대상</span><input value={target} onChange={e => setTarget(e.target.value)} placeholder="전체 사용자 / 부전1동 A구역" /></label><label><span>예약 시각</span><input type="datetime-local" value={at} onChange={e => setAt(e.target.value)} /></label></div>
    </Modal>
  </>;
}

function SettingsPage() {
  return <>
    <PageHeader title="관리자 설정" subtitle="관리자 웹 실행 환경과 보안 설정 기준을 확인합니다." />
    <div className="settings">
      <article><i>⇄</i><div><b>Backend API</b><span>현재 연결 주소</span><code>{CONFIG.apiBaseUrl}</code></div></article>
      <article><i>◈</i><div><b>실행 모드</b><span>관리자 API 연결 상태</span><code>{CONFIG.useMocks ? 'DEMO / MOCK' : 'REAL API'}</code></div></article>
      <article><i>⌘</i><div><b>JWT 보안</b><span>Secret은 웹에 포함하지 않음</span><code>Backend Secret Only</code></div></article>
    </div>
    <section className="panel security"><h2>보안 체크</h2><ul><li>JWT Secret, 공공데이터 API Key, Kakao Key는 `application-secret.yml`에만 저장합니다.</li><li>관리자 권한은 프론트 화면 숨김이 아니라 Spring Security에서 반드시 재검증합니다.</li><li>사용자 AI 정정값은 관리자 검수 후 승인 데이터만 재학습 후보로 사용합니다.</li></ul></section>
  </>;
}
