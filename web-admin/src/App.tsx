import {
  Fragment,
  type FormEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  Navigate,
  NavLink,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { adminApi, authApi } from "./api";
import { CONFIG } from "./config";
import type {
  AiCorrection,
  ApartmentSchedule,
  AreaScheduleCoverage,
  AreaScheduleGroupCoverage,
  CollectionArea,
  CollectionWasteType,
  Dashboard,
  GeneralHousingSchedule,
  Guide,
  Notification,
  Residence,
  ReviewStatus,
  Schedule,
  SyncLog,
  User,
  UserStatus,
  WasteItem,
} from "./types";

const icons: Record<string, string> = {
  dashboard: "▦",
  users: "♙",
  residence: "⌂",
  area: "⌖",
  schedule: "◷",
  waste: "♻",
  guide: "✓",
  ai: "✦",
  data: "⇄",
  notification: "◉",
  settings: "⚙",
};

function Badge({ value }: { value: string }) {
  const key = value.toUpperCase();
  const good = ["ACTIVE", "APPROVED", "SUCCESS", "SENT"].includes(key);
  const warn = ["PENDING", "RUNNING", "SCHEDULED", "DRAFT"].includes(key);
  const bad = ["REJECTED", "FAILED", "SUSPENDED"].includes(key);
  const labels: Record<string, string> = {
    ACTIVE: "활성",
    INACTIVE: "비활성",
    SUSPENDED: "정지",
    WITHDRAWN: "탈퇴",
    APPROVED: "승인",
    PENDING: "대기",
    REJECTED: "거절",
    SUCCESS: "성공",
    FAILED: "실패",
    RUNNING: "진행 중",
    SENT: "발송 완료",
    CANCELLED: "발송 취소",
    SCHEDULED: "예약",
    DRAFT: "임시저장",
    USER: "사용자",
    ADMIN: "관리자",
  };
  return (
    <span
      className={`badge ${good ? "good" : warn ? "warn" : bad ? "bad" : ""}`}
    >
      {labels[key] ?? value}
    </span>
  );
}

function PageHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle: string;
  action?: ReactNode;
}) {
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

function Modal({
  open,
  title,
  children,
  onClose,
  footer,
}: {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
  footer?: ReactNode;
}) {
  if (!open) return null;
  return (
    <div
      className="modal-bg"
      onMouseDown={(e) => e.currentTarget === e.target && onClose()}
    >
      <div className="modal">
        <div className="modal-head">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose}>
            ×
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

function Loading() {
  return (
    <div className="state-card">
      <span className="spinner" /> 데이터를 불러오고 있어요.
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return <div className="state-card muted">{text}</div>;
}

function TableWrap({ children }: { children: ReactNode }) {
  return (
    <div className="table-wrap">
      <div className="table-scroll">{children}</div>
    </div>
  );
}

type Session = { email: string; role: string } | null;

export default function App() {
  const [session, setSession] = useState<Session>(() => authApi.session());

  useEffect(() => {
    const expired = () => setSession(null);
    window.addEventListener("smartrecycle-auth-expired", expired);
    return () =>
      window.removeEventListener("smartrecycle-auth-expired", expired);
  }, []);

  return (
    <Routes>
      <Route
        path="/login"
        element={<LoginPage session={session} onLogin={setSession} />}
      />
      <Route
        path="/*"
        element={
          session ? (
            <Shell
              session={session}
              onLogout={() => {
                authApi.logout();
                setSession(null);
              }}
            />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
    </Routes>
  );
}

function LoginPage({
  session,
  onLogin,
}: {
  session: Session;
  onLogin: (session: NonNullable<Session>) => void;
}) {
  const navigate = useNavigate();
  const [email, setEmail] = useState(
    CONFIG.useMocks ? "admin@smartrecycle.com" : "",
  );
  const [password, setPassword] = useState(CONFIG.useMocks ? "Admin1234!" : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  if (session) return <Navigate to="/dashboard" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const next = await authApi.login(email.trim(), password);
      onLogin(next);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <section className="login-hero">
        <div className="login-mark">♻</div>
        <div className="eyebrow light">SMARTRECYCLE</div>
        <h1>
          더 정확한 분리배출을
          <br />
          위한 운영 콘솔
        </h1>
        <p>
          사용자, 거주지, 배출 일정, 폐기물 가이드와 AI 정정 데이터까지 한
          곳에서 관리합니다.
        </p>
        <div className="hero-tags">
          <span>관리자 전용</span>
          <span>JWT 인증</span>
          <span>AI 검수</span>
        </div>
      </section>
      <section className="login-side">
        <form className="login-card" onSubmit={submit}>
          <div className="eyebrow">ADMIN LOGIN</div>
          <h2>관리자 로그인</h2>
          <p>운영 계정으로 로그인해주세요.</p>
          {CONFIG.useMocks && (
            <div className="demo">
              데모 계정
              <br />
              <b>admin@smartrecycle.com</b>
              <br />
              Admin1234!
            </div>
          )}
          {error && <div className="error-box">{error}</div>}
          <label>
            <span>이메일</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label>
            <span>비밀번호</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          <button className="btn primary login-btn" disabled={busy}>
            {busy ? "로그인 중..." : "관리자 로그인 →"}
          </button>
        </form>
      </section>
    </div>
  );
}

const navGroups: Array<[string, Array<[string, string, string]>]> = [
  ["개요", [["/dashboard", "dashboard", "대시보드"]]],
  [
    "사용자·거주지",
    [
      ["/users", "users", "사용자 관리"],
      ["/residences", "residence", "공동주택 관리"],
      ["/collection-areas", "area", "수거구역 관리"],
      ["/schedules", "schedule", "배출 일정 관리"],
    ],
  ],
  [
    "분리배출 정보",
    [
      ["/waste-items", "waste", "폐기물 품목"],
      ["/guides", "guide", "분리배출 가이드"],
    ],
  ],
  [
    "AI·운영",
    [
      ["/ai-corrections", "ai", "AI 정정 검수"],
      ["/public-data", "data", "공공데이터"],
      ["/notifications", "notification", "알림 관리"],
    ],
  ],
];

function Shell({
  session,
  onLogout,
}: {
  session: NonNullable<Session>;
  onLogout: () => void;
}) {
  const [mobile, setMobile] = useState(false);
  const location = useLocation();

  return (
    <div className="layout">
      <aside className={`sidebar ${mobile ? "open" : ""}`}>
        <div className="brand">
          <span>♻</span>
          <div>
            <b>SmartRecycle</b>
            <small>ADMIN CONSOLE</small>
          </div>
        </div>
        <nav>
          {navGroups.map(([group, items]) => (
            <div className="nav-group" key={group}>
              <div className="nav-label">{group}</div>
              {items.map(([to, icon, label]) => (
                <NavLink
                  key={to}
                  to={to}
                  onClick={() => setMobile(false)}
                  className={({ isActive }) =>
                    `nav-item ${isActive ? "active" : ""}`
                  }
                >
                  <span className="nav-icon">{icons[icon]}</span>
                  {label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <NavLink to="/settings" className="nav-item">
            <span className="nav-icon">{icons.settings}</span>관리자 설정
          </NavLink>
          <button className="nav-item logout" onClick={onLogout}>
            <span className="nav-icon">↪</span>로그아웃
          </button>
        </div>
      </aside>
      {mobile && (
        <button className="backdrop" onClick={() => setMobile(false)} />
      )}
      <main>
        <header className="topbar">
          <div className="top-left">
            <button className="mobile-menu" onClick={() => setMobile(true)}>
              ☰
            </button>
            <div>
              <b>{routeTitle(location.pathname)}</b>
              <small>SmartRecycle 운영 관리</small>
            </div>
          </div>
          <div className="top-right">
            {CONFIG.useMocks && <span className="demo-pill">DEMO MODE</span>}
            <div className="avatar">관</div>
            <div className="profile">
              <b>관리자</b>
              <small>{session.email}</small>
            </div>
          </div>
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
    "/dashboard": "대시보드",
    "/users": "사용자 관리",
    "/residences": "거주지 관리",
    "/collection-areas": "수거구역 관리",
    "/schedules": "배출 일정 관리",
    "/waste-items": "폐기물 품목 관리",
    "/guides": "분리배출 가이드 관리",
    "/ai-corrections": "AI 정정 검수",
    "/public-data": "공공데이터 관리",
    "/notifications": "알림 관리",
    "/settings": "관리자 설정",
  };
  return map[path] ?? "SmartRecycle";
}

function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  useEffect(() => {
    adminApi.dashboard().then(setData);
  }, []);
  if (!data) return <Loading />;

  const stats = [
    ["전체 사용자", data.users, `활성 ${data.activeUsers}명`, "users"],
    [
      "승인 대기 거주지",
      data.pendingResidences,
      "관리자 확인 필요",
      "residence",
    ],
    ["등록 폐기물 품목", data.wasteItems, "검색·가이드 대상", "waste"],
    ["AI 정정 검수 대기", data.pendingAi, "재학습 후보 검수", "ai"],
    [
      "오늘 발송 알림",
      data.todayNotifications,
      "오늘 실제 발송 건수",
      "notification",
    ],
  ];

  return (
    <>
      <PageHeader
        title="운영 현황"
        subtitle="SmartRecycle 서비스의 핵심 상태를 빠르게 확인합니다."
      />
      <div className="stats">
        {stats.map(([label, value, helper, icon]) => (
          <article className="stat" key={String(label)}>
            <div>
              <span>{label}</span>
              <i>{icons[String(icon)]}</i>
            </div>
            <strong>{value}</strong>
            <small>{helper}</small>
          </article>
        ))}
      </div>
      <div className="dash-grid">
        <section className="panel">
          <h2>관리자 우선 처리 항목</h2>
          <div className="todo-list">
            <div>
              <b>AI 정정 검수</b>
              <span>{data.pendingAi}건 대기</span>
            </div>
            <div>
              <b>거주지 승인</b>
              <span>{data.pendingResidences}건 대기</span>
            </div>
            <div>
              <b>오늘 발송 알림</b>
              <span>{data.todayNotifications}건</span>
            </div>
          </div>
        </section>
        <section className="panel">
          <h2>현재 운영 구조</h2>
          <div className="flow">
            <span>공공데이터</span>
            <b>→</b>
            <span>수거구역·일정</span>
            <b>→</b>
            <span>사용자 맞춤 안내</span>
          </div>
          <div className="flow">
            <span>AI 추정</span>
            <b>→</b>
            <span>사용자 정정</span>
            <b>→</b>
            <span>관리자 검수</span>
          </div>
        </section>
      </div>
    </>
  );
}

function UsersPage() {
  const PAGE_SIZE = 20;

  const [items, setItems] = useState<User[]>([]);
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const [selected, setSelected] = useState<User | null>(null);
  const [draftStatus, setDraftStatus] = useState<UserStatus>("ACTIVE");
  const [draftRole, setDraftRole] = useState<"USER" | "ADMIN">("USER");

  async function load() {
    setLoading(true);
    setError("");

    try {
      const result = await adminApi.users({
        keyword: query,
        page: page - 1,
        size: PAGE_SIZE,
      });

      setItems(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);

      if (result.totalPages > 0 && page > result.totalPages) {
        setPage(result.totalPages);
      }
    } catch (err) {
      setItems([]);
      setTotalElements(0);
      setTotalPages(0);
      setError(
        err instanceof Error
          ? err.message
          : "사용자 목록을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [page, query]);

  function applySearch() {
    setPage(1);
    setQuery(queryInput.trim());
  }

  function resetSearch() {
    setQueryInput("");
    setQuery("");
    setPage(1);
  }

  function openUser(user: User) {
    setSelected(user);
    setDraftStatus(user.status);
    setDraftRole(user.role);
  }

  async function saveUser() {
    if (!selected) return;

    const statusChanged = draftStatus !== selected.status;
    const roleChanged = draftRole !== selected.role;

    if (!statusChanged && !roleChanged) {
      setSelected(null);
      return;
    }

    const changes: string[] = [];

    if (statusChanged) {
      const statusLabel: Record<UserStatus, string> = {
        ACTIVE: "활성",
        SUSPENDED: "정지",
        WITHDRAWN: "탈퇴",
      };
      changes.push(
        `상태: ${statusLabel[selected.status]} → ${statusLabel[draftStatus]}`,
      );
    }

    if (roleChanged) {
      const roleLabel = {
        USER: "사용자",
        ADMIN: "관리자",
      };
      changes.push(
        `역할: ${roleLabel[selected.role]} → ${roleLabel[draftRole]}`,
      );
    }

    if (
      !window.confirm(
        `${selected.name} 계정을 다음과 같이 변경할까요?\n\n${changes.join("\n")}`,
      )
    ) {
      return;
    }

    setSaving(true);

    try {
      if (statusChanged) {
        await adminApi.setUserStatus(selected.id, draftStatus);
      }

      if (roleChanged) {
        await adminApi.setUserRole(selected.id, draftRole);
      }

      setSelected(null);
      await load();
    } catch (err) {
      window.alert(
        err instanceof Error ? err.message : "사용자 정보 변경에 실패했습니다.",
      );
    } finally {
      setSaving(false);
    }
  }

  const visiblePages = useMemo(() => {
    if (totalPages <= 0) return [];

    let start = Math.max(1, page - 2);
    let end = Math.min(totalPages, start + 4);

    if (end - start < 4) {
      start = Math.max(1, end - 4);
    }

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [page, totalPages]);

  return (
    <>
      <PageHeader
        title="사용자 관리"
        subtitle="계정 상태, 역할, 거주지 연결 정보를 관리합니다."
      />

      <section className="user-filter-panel">
        <div className="user-filter-row">
          <label className="user-search-field">
            <span>사용자 검색</span>
            <input
              className="search"
              placeholder="이메일, 이름, 주소 검색"
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  applySearch();
                }
              }}
            />
          </label>

          <div className="user-filter-actions">
            <button className="btn primary" onClick={applySearch}>
              검색
            </button>

            <button className="btn" onClick={resetSearch}>
              초기화
            </button>
          </div>
        </div>

        <div className="area-result-summary">
          <span>
            전체 사용자 <b>{totalElements.toLocaleString()}</b>명
          </span>

          <span>페이지당 {PAGE_SIZE}명</span>

          {query && (
            <span>
              검색어 <b>“{query}”</b>
            </span>
          )}
        </div>
      </section>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <Loading />
      ) : items.length === 0 ? (
        <Empty text="검색 결과가 없습니다." />
      ) : (
        <>
          <TableWrap>
            <table className="users-table">
              <thead>
                <tr>
                  <th>사용자</th>
                  <th>역할</th>
                  <th>상태</th>
                  <th>거주지</th>
                  <th>주소</th>
                  <th>가입일</th>
                  <th />
                </tr>
              </thead>

              <tbody>
                {items.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <b>{user.name}</b>
                      <small>{user.email}</small>
                    </td>

                    <td>
                      <Badge value={user.role} />
                    </td>

                    <td>
                      <Badge value={user.status} />
                    </td>

                    <td>{user.residence || "미설정"}</td>

                    <td>
                      <div className="user-address">{user.address || "-"}</div>
                    </td>

                    <td>{user.createdAt}</td>

                    <td className="right">
                      <button
                        className="btn small"
                        onClick={() => openUser(user)}
                      >
                        관리
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>

          <div className="pagination-bar">
            <div className="pagination-summary">
              {totalPages > 0 ? `${page} / ${totalPages} 페이지` : "0 페이지"}
            </div>

            <div className="pagination">
              <button
                className="page-btn"
                disabled={page <= 1}
                onClick={() => setPage((value) => Math.max(1, value - 1))}
              >
                ‹ 이전
              </button>

              {visiblePages.map((pageNumber) => (
                <button
                  key={pageNumber}
                  className={`page-btn ${pageNumber === page ? "active" : ""}`}
                  onClick={() => setPage(pageNumber)}
                >
                  {pageNumber}
                </button>
              ))}

              <button
                className="page-btn"
                disabled={totalPages === 0 || page >= totalPages}
                onClick={() =>
                  setPage((value) => Math.min(totalPages, value + 1))
                }
              >
                다음 ›
              </button>
            </div>
          </div>
        </>
      )}

      <Modal
        open={!!selected}
        title="사용자 상세 관리"
        onClose={() => {
          if (!saving) {
            setSelected(null);
          }
        }}
        footer={
          <>
            <button
              className="btn"
              disabled={saving}
              onClick={() => setSelected(null)}
            >
              취소
            </button>

            <button
              className="btn primary"
              disabled={saving}
              onClick={saveUser}
            >
              {saving ? "저장 중..." : "변경사항 저장"}
            </button>
          </>
        }
      >
        {selected && (
          <div className="form-grid">
            <div className="info">
              <span>이름</span>
              <b>{selected.name}</b>
            </div>

            <div className="info">
              <span>이메일</span>
              <b>{selected.email}</b>
            </div>

            <div className="info">
              <span>가입일</span>
              <b>{selected.createdAt}</b>
            </div>

            <div className="info">
              <span>현재 거주지</span>
              <b>{selected.residence || "미설정"}</b>
            </div>

            <div className="info full">
              <span>주소</span>
              <b>{selected.address || "-"}</b>
            </div>

            <label>
              <span>계정 상태</span>
              <select
                value={draftStatus}
                disabled={saving}
                onChange={(event) =>
                  setDraftStatus(event.target.value as UserStatus)
                }
              >
                <option value="ACTIVE">활성</option>
                <option value="SUSPENDED">정지</option>
                <option value="WITHDRAWN">탈퇴</option>
              </select>
            </label>

            <label>
              <span>역할</span>
              <select
                value={draftRole}
                disabled={saving}
                onChange={(event) =>
                  setDraftRole(event.target.value as "USER" | "ADMIN")
                }
              >
                <option value="USER">사용자</option>
                <option value="ADMIN">관리자</option>
              </select>
            </label>

            <div className="user-admin-warning full">
              <b>관리자 변경 주의</b>
              <span>
                계정 정지·탈퇴 또는 관리자 권한 부여는 즉시 서비스 권한에 영향을
                줄 수 있습니다. 변경 내용을 확인한 뒤 저장해주세요.
              </span>
            </div>
          </div>
        )}
      </Modal>
    </>
  );
}

function ResidencesPage() {
  const [items, setItems] = useState<Residence[]>([]);
  const [loading, setLoading] = useState(true);
  const load = async () => {
    setLoading(true);
    setItems(await adminApi.residences());
    setLoading(false);
  };
  useEffect(() => {
    load();
  }, []);
  async function review(id: number, ok: boolean) {
    await adminApi.reviewResidence(id, ok);
    await load();
  }
  return (
    <>
      <PageHeader
        title="공동주택 관리"
        subtitle="아파트·오피스텔 등 공동주택 등록 요청을 승인하거나 거절합니다."
      />
      {loading ? (
        <Loading />
      ) : (
        <TableWrap>
          <table>
            <thead>
              <tr>
                <th>거주지</th>
                <th>유형</th>
                <th>주소</th>
                <th>건물관리번호</th>
                <th>수거구역</th>
                <th>상태</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((x) => (
                <tr key={x.id}>
                  <td>
                    <b>{x.name}</b>
                  </td>
                  <td>{x.type}</td>
                  <td>{x.address}</td>
                  <td>{x.buildingNo ?? "-"}</td>
                  <td>{x.area ?? "미연결"}</td>
                  <td>
                    <Badge value={x.approval} />
                  </td>
                  <td className="right">
                    {x.approval === "PENDING" && (
                      <div className="actions">
                        <button
                          className="btn small primary"
                          onClick={() => review(x.id, true)}
                        >
                          승인
                        </button>
                        <button
                          className="btn small danger"
                          onClick={() => review(x.id, false)}
                        >
                          거절
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableWrap>
      )}
    </>
  );
}

function AreasPage() {
  const PAGE_SIZE = 20;

  const [items, setItems] = useState<CollectionArea[]>([]);
  const [editing, setEditing] = useState<CollectionArea | null>(null);
  const [creating, setCreating] = useState(false);

  const [name, setName] = useState("");
  const [district, setDistrict] = useState("부산진구");
  const [dongs, setDongs] = useState("");

  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [sourceFilter, setSourceFilter] = useState<
    "" | "MOIS_HOUSEHOLD_WASTE" | "MANUAL"
  >("");
  const [activeFilter, setActiveFilter] = useState<"" | "true" | "false">("");

  const [page, setPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function load() {
    setLoading(true);
    setError("");

    try {
      const result = await adminApi.areas({
        keyword: query,
        sourceType: sourceFilter,
        active: activeFilter,
        page: page - 1,
        size: PAGE_SIZE,
      });

      setItems(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);

      if (result.totalPages > 0 && page > result.totalPages) {
        setPage(result.totalPages);
      }
    } catch (err) {
      setItems([]);
      setTotalElements(0);
      setTotalPages(0);
      setError(
        err instanceof Error ? err.message : "수거구역을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [page, query, sourceFilter, activeFilter]);

  function applySearch() {
    setPage(1);
    setQuery(queryInput.trim());
  }

  function resetFilters() {
    setQueryInput("");
    setQuery("");
    setSourceFilter("");
    setActiveFilter("");
    setPage(1);
  }

  function open(item?: CollectionArea) {
    if (item) {
      if (item.sourceType !== "MANUAL") {
        window.alert(
          "공공데이터 수거구역은 직접 수정할 수 없습니다. 공공데이터 동기화로 관리해주세요.",
        );
        return;
      }

      setEditing(item);
      setName(item.name);
      setDistrict(item.district);
      setDongs(item.dongs.join(", "));
    } else {
      setEditing(null);
      setName("");
      setDistrict("부산진구");
      setDongs("");
    }

    setCreating(true);
  }

  async function save() {
    try {
      await adminApi.saveArea({
        id: editing?.id,
        name,
        district,
        dongs: dongs
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
        active: editing?.active ?? true,
      });

      setCreating(false);
      await load();
    } catch (err) {
      window.alert(
        err instanceof Error ? err.message : "수거구역 저장에 실패했습니다.",
      );
    }
  }

  async function toggleActive(item: CollectionArea) {
    const nextActive = !item.active;
    const action = nextActive ? "활성화" : "비활성화";

    if (!window.confirm(`${item.name} 수거구역을 ${action}할까요?`)) {
      return;
    }

    try {
      await adminApi.setAreaActive(item.id, nextActive);
      await load();
    } catch (err) {
      window.alert(
        err instanceof Error
          ? err.message
          : `수거구역 ${action}에 실패했습니다.`,
      );
    }
  }

  const visiblePages = useMemo(() => {
    if (totalPages <= 0) return [];

    const maxButtons = 5;
    let start = Math.max(1, page - Math.floor(maxButtons / 2));
    let end = Math.min(totalPages, start + maxButtons - 1);

    if (end - start + 1 < maxButtons) {
      start = Math.max(1, end - maxButtons + 1);
    }

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [page, totalPages]);

  const wasteTypeLabel = (value: string) => {
    const labels: Record<string, string> = {
      LIFE_WASTE: "생활쓰레기",
      FOOD_WASTE: "음식물",
      RECYCLABLE: "재활용",
    };

    return labels[value] ?? value;
  };

  return (
    <>
      <PageHeader
        title="수거구역 관리"
        subtitle="공공데이터 수거구역을 검색·필터링하고 필요한 구역을 관리자가 직접 추가합니다."
        action={
          <button className="btn primary" onClick={() => open()}>
            + 수거구역 추가
          </button>
        }
      />

      <section className="area-filter-panel">
        <div className="area-filter-grid">
          <label className="area-search-field">
            <span>지역·구역 검색</span>
            <input
              className="search"
              placeholder="시/도, 시/군/구, 구역명, 대상지역, 관리번호"
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              onKeyDown={(event) => event.key === "Enter" && applySearch()}
            />
          </label>

          <label>
            <span>출처</span>
            <select
              value={sourceFilter}
              onChange={(event) => {
                setPage(1);
                setSourceFilter(
                  event.target.value as "" | "MOIS_HOUSEHOLD_WASTE" | "MANUAL",
                );
              }}
            >
              <option value="">전체 출처</option>
              <option value="MOIS_HOUSEHOLD_WASTE">공공데이터</option>
              <option value="MANUAL">관리자 직접 등록</option>
            </select>
          </label>

          <label>
            <span>상태</span>
            <select
              value={activeFilter}
              onChange={(event) => {
                setPage(1);
                setActiveFilter(event.target.value as "" | "true" | "false");
              }}
            >
              <option value="">전체 상태</option>
              <option value="true">활성</option>
              <option value="false">비활성</option>
            </select>
          </label>

          <div className="area-filter-actions">
            <button className="btn primary" onClick={applySearch}>
              검색
            </button>
            <button className="btn" onClick={resetFilters}>
              초기화
            </button>
          </div>
        </div>

        <div className="area-result-summary">
          <span>
            총 <b>{totalElements.toLocaleString()}</b>개
          </span>
          <span>페이지당 {PAGE_SIZE}개</span>
          {query && (
            <span>
              검색어 <b>“{query}”</b>
            </span>
          )}
        </div>
      </section>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <Loading />
      ) : items.length === 0 ? (
        <Empty text="조건에 맞는 수거구역이 없습니다." />
      ) : (
        <>
          <TableWrap>
            <table className="area-table">
              <thead>
                <tr>
                  <th>구역</th>
                  <th>시/도</th>
                  <th>시/군/구</th>
                  <th>대상지역</th>
                  <th>수거종류</th>
                  <th>출처</th>
                  <th>상태</th>
                  <th>최근 갱신</th>
                  <th />
                </tr>
              </thead>

              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <b>{item.name}</b>
                      <small>
                        관리번호: {item.externalManagementNumber || "-"}
                      </small>
                    </td>

                    <td>{item.sido || "-"}</td>

                    <td>{item.district || "-"}</td>

                    <td>
                      <div className="area-target-text">
                        {item.dongs.length
                          ? item.dongs.join(", ")
                          : item.targetAreaName || "-"}
                      </div>
                    </td>

                    <td>
                      <div className="waste-type-badges">
                        {item.wasteTypes?.length ? (
                          item.wasteTypes.map((wasteType) => (
                            <span className="mini-chip" key={wasteType}>
                              {wasteTypeLabel(wasteType)}
                            </span>
                          ))
                        ) : (
                          <span className="muted">-</span>
                        )}
                      </div>
                    </td>

                    <td>
                      <span
                        className={`source-chip ${
                          item.sourceType === "MANUAL" ? "manual" : ""
                        }`}
                      >
                        {item.sourceType === "MANUAL"
                          ? "관리자 직접 등록"
                          : "공공데이터"}
                      </span>
                    </td>

                    <td>
                      <Badge value={item.active ? "ACTIVE" : "INACTIVE"} />
                    </td>

                    <td>{item.updatedAt}</td>

                    <td className="right">
                      <div className="actions">
                        {item.sourceType === "MANUAL" && (
                          <button
                            className="btn small"
                            onClick={() => open(item)}
                          >
                            수정
                          </button>
                        )}

                        <button
                          className={`btn small ${
                            item.active ? "danger" : "primary"
                          }`}
                          onClick={() => toggleActive(item)}
                        >
                          {item.active ? "비활성화" : "활성화"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>

          <div className="pagination-bar">
            <div className="pagination-summary">
              {totalPages > 0 ? `${page} / ${totalPages} 페이지` : "0 페이지"}
            </div>

            <div className="pagination">
              <button
                className="page-btn"
                disabled={page <= 1}
                onClick={() => setPage((value) => Math.max(1, value - 1))}
              >
                ‹ 이전
              </button>

              {visiblePages.map((pageNumber) => (
                <button
                  key={pageNumber}
                  className={`page-btn ${pageNumber === page ? "active" : ""}`}
                  onClick={() => setPage(pageNumber)}
                >
                  {pageNumber}
                </button>
              ))}

              <button
                className="page-btn"
                disabled={totalPages === 0 || page >= totalPages}
                onClick={() =>
                  setPage((value) => Math.min(totalPages, value + 1))
                }
              >
                다음 ›
              </button>
            </div>
          </div>
        </>
      )}

      <Modal
        open={creating}
        title={editing ? "수거구역 수정" : "수거구역 추가"}
        onClose={() => setCreating(false)}
        footer={
          <>
            <button className="btn" onClick={() => setCreating(false)}>
              취소
            </button>
            <button
              className="btn primary"
              onClick={save}
              disabled={!name.trim() || !district.trim()}
            >
              저장
            </button>
          </>
        }
      >
        <div className="form-grid">
          <label>
            <span>구역명</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>

          <label>
            <span>시/군/구</span>
            <input
              value={district}
              onChange={(event) => setDistrict(event.target.value)}
            />
          </label>

          <label className="full">
            <span>대상 행정동 (쉼표 구분)</span>
            <input
              value={dongs}
              onChange={(event) => setDongs(event.target.value)}
              placeholder="부전1동, 부전2동"
            />
          </label>
        </div>
      </Modal>
    </>
  );
}

function SchedulesPage() {
  const PAGE_SIZE = 20;

  const [tab, setTab] = useState<"GENERAL" | "APARTMENT">("GENERAL");

  const wasteTypeLabels: Record<CollectionWasteType, string> = {
    LIFE_WASTE: "생활쓰레기",
    FOOD_WASTE: "음식물",
    RECYCLABLE: "재활용",
  };

  const dayLabels: Record<string, string> = {
    MONDAY: "월요일",
    TUESDAY: "화요일",
    WEDNESDAY: "수요일",
    THURSDAY: "목요일",
    FRIDAY: "금요일",
    SATURDAY: "토요일",
    SUNDAY: "일요일",
  };

  /* 일반주택 */
  const [coverageGroups, setCoverageGroups] = useState<
    AreaScheduleGroupCoverage[]
  >([]);

  const [generalPage, setGeneralPage] = useState(1);

  const [generalTotalElements, setGeneralTotalElements] = useState(0);

  const [generalTotalPages, setGeneralTotalPages] = useState(0);

  const [generalQueryInput, setGeneralQueryInput] = useState("");

  const [generalQuery, setGeneralQuery] = useState("");

  const [generalSource, setGeneralSource] = useState<
    "" | "MOIS_HOUSEHOLD_WASTE" | "MANUAL"
  >("");

  const [generalActive, setGeneralActive] = useState<"" | "true" | "false">("");

  const [generalLoading, setGeneralLoading] = useState(false);

  const [generalError, setGeneralError] = useState("");

  const [generalModalOpen, setGeneralModalOpen] = useState(false);

  const [editingGeneral, setEditingGeneral] =
    useState<GeneralHousingSchedule | null>(null);

  const [generalArea, setGeneralArea] = useState<AreaScheduleCoverage | null>(
    null,
  );

  const [generalWasteType, setGeneralWasteType] =
    useState<CollectionWasteType>("RECYCLABLE");

  const [generalDays, setGeneralDays] = useState("");

  const [generalStartTime, setGeneralStartTime] = useState("");

  const [generalEndTime, setGeneralEndTime] = useState("");

  async function loadGeneral() {
    setGeneralLoading(true);
    setGeneralError("");

    try {
      const result = await adminApi.generalScheduleCoverage({
        keyword: generalQuery,
        sourceType: generalSource,
        active: generalActive,
        page: generalPage - 1,
        size: PAGE_SIZE,
      });

      setCoverageGroups(result.items);

      setGeneralTotalElements(result.totalElements);

      setGeneralTotalPages(result.totalPages);

      if (result.totalPages > 0 && generalPage > result.totalPages) {
        setGeneralPage(result.totalPages);
      }
    } catch (error) {
      setCoverageGroups([]);
      setGeneralTotalElements(0);
      setGeneralTotalPages(0);

      setGeneralError(
        error instanceof Error
          ? error.message
          : "일반주택 일정을 불러오지 못했습니다.",
      );
    } finally {
      setGeneralLoading(false);
    }
  }

  useEffect(() => {
    if (tab === "GENERAL") {
      void loadGeneral();
    }
  }, [tab, generalPage, generalQuery, generalSource, generalActive]);

  function openGeneralCreate(
    area: AreaScheduleCoverage,
    preferredWasteType?: CollectionWasteType,
  ) {
    setEditingGeneral(null);
    setGeneralArea(area);

    setGeneralWasteType(
      preferredWasteType ??
        area.missingWasteTypes[0] ??
        area.supportedWasteTypes[0] ??
        "RECYCLABLE",
    );

    setGeneralDays("");
    setGeneralStartTime("");
    setGeneralEndTime("");
    setGeneralModalOpen(true);
  }

  function openGeneralEdit(
    area: AreaScheduleCoverage,
    schedule: GeneralHousingSchedule,
  ) {
    setGeneralArea(area);
    setEditingGeneral(schedule);
    setGeneralWasteType(schedule.wasteType);
    setGeneralDays(schedule.day);
    setGeneralStartTime(schedule.startTime ?? "");
    setGeneralEndTime(schedule.endTime ?? "");
    setGeneralModalOpen(true);
  }

  async function saveGeneral() {
    if (!generalArea || !generalDays.trim()) {
      return;
    }

    if (Boolean(generalStartTime) !== Boolean(generalEndTime)) {
      window.alert(
        "시작 시간과 종료 시간은 둘 다 입력하거나 둘 다 비워주세요.",
      );
      return;
    }

    try {
      await adminApi.saveGeneralSchedule({
        id: editingGeneral?.id,
        collectionAreaId: generalArea.collectionAreaId,
        wasteType: generalWasteType,
        emissionDays: generalDays.trim(),
        startTime: generalStartTime || undefined,
        endTime: generalEndTime || undefined,
      });

      setGeneralModalOpen(false);

      await loadGeneral();
    } catch (error) {
      window.alert(
        error instanceof Error ? error.message : "일정 저장에 실패했습니다.",
      );
    }
  }

  async function removeGeneral(schedule: GeneralHousingSchedule) {
    if (
      !window.confirm(
        `${schedule.collectionAreaName}의 ${wasteTypeLabels[schedule.wasteType]} 일정을 삭제할까요?`,
      )
    ) {
      return;
    }

    try {
      await adminApi.deleteGeneralSchedule(schedule.id);

      await loadGeneral();
    } catch (error) {
      window.alert(
        error instanceof Error ? error.message : "일정 삭제에 실패했습니다.",
      );
    }
  }

  const [expandedGeneralGroups, setExpandedGeneralGroups] = useState<
    Set<string>
  >(() => new Set());

  /*
   * 이제 백엔드가 먼저 지역 그룹을 만든 뒤
   * 그 지역 그룹 자체를 페이지네이션해서 내려줍니다.
   *
   * 프론트에서 현재 페이지의 CollectionArea 20개를
   * 다시 묶는 작업은 하지 않습니다.
   */
  const generalGroups = coverageGroups;

  function toggleGeneralGroup(groupKey: string) {
    setExpandedGeneralGroups((current) => {
      const next = new Set(current);

      if (next.has(groupKey)) {
        next.delete(groupKey);
      } else {
        next.add(groupKey);
      }

      return next;
    });
  }

  const generalPages = useMemo(() => {
    if (generalTotalPages <= 0) {
      return [];
    }

    let start = Math.max(1, generalPage - 2);

    let end = Math.min(generalTotalPages, start + 4);

    if (end - start < 4) {
      start = Math.max(1, end - 4);
    }

    return Array.from(
      {
        length: end - start + 1,
      },
      (_, index) => start + index,
    );
  }, [generalPage, generalTotalPages]);

  /* 공동주택 */
  const [apartments, setApartments] = useState<Residence[]>([]);

  const [apartmentKeyword, setApartmentKeyword] = useState("");

  const [selectedApartmentId, setSelectedApartmentId] = useState<number | null>(
    null,
  );

  const [apartmentSchedules, setApartmentSchedules] = useState<
    ApartmentSchedule[]
  >([]);

  const [apartmentWasteItems, setApartmentWasteItems] = useState<WasteItem[]>(
    [],
  );

  const [apartmentLoading, setApartmentLoading] = useState(false);

  const [apartmentError, setApartmentError] = useState("");

  const [apartmentModalOpen, setApartmentModalOpen] = useState(false);

  const [editingApartmentSchedule, setEditingApartmentSchedule] =
    useState<ApartmentSchedule | null>(null);

  const [apartmentWasteItemId, setApartmentWasteItemId] = useState(0);

  const [apartmentDay, setApartmentDay] = useState("MONDAY");

  const [apartmentStartTime, setApartmentStartTime] = useState("18:00");

  const [apartmentEndTime, setApartmentEndTime] = useState("21:00");

  const [apartmentAlwaysAvailable, setApartmentAlwaysAvailable] =
    useState(false);

  useEffect(() => {
    if (tab !== "APARTMENT") {
      return;
    }

    void (async () => {
      try {
        const [residenceItems, wasteItems] = await Promise.all([
          adminApi.residences(),
          adminApi.wasteItems(""),
        ]);

        const approved = residenceItems.filter(
          (item) => item.approval === "APPROVED",
        );

        setApartments(approved);

        setApartmentWasteItems(wasteItems.filter((item) => item.active));

        if (selectedApartmentId === null && approved.length > 0) {
          setSelectedApartmentId(approved[0].id);
        }
      } catch (error) {
        setApartmentError(
          error instanceof Error
            ? error.message
            : "공동주택 목록을 불러오지 못했습니다.",
        );
      }
    })();
  }, [tab]);

  async function loadApartmentSchedules(apartmentId: number) {
    setApartmentLoading(true);
    setApartmentError("");

    try {
      setApartmentSchedules(await adminApi.apartmentSchedules(apartmentId));
    } catch (error) {
      setApartmentSchedules([]);

      setApartmentError(
        error instanceof Error
          ? error.message
          : "공동주택 일정을 불러오지 못했습니다.",
      );
    } finally {
      setApartmentLoading(false);
    }
  }

  useEffect(() => {
    if (tab === "APARTMENT" && selectedApartmentId !== null) {
      void loadApartmentSchedules(selectedApartmentId);
    }
  }, [tab, selectedApartmentId]);

  const filteredApartments = useMemo(() => {
    const keyword = apartmentKeyword.trim().toLowerCase();

    if (!keyword) {
      return apartments;
    }

    return apartments.filter((item) =>
      `${item.name} ${item.address}`.toLowerCase().includes(keyword),
    );
  }, [apartments, apartmentKeyword]);

  const selectedApartment = apartments.find(
    (item) => item.id === selectedApartmentId,
  );

  function openApartmentCreate() {
    if (selectedApartmentId === null) {
      return;
    }

    setEditingApartmentSchedule(null);

    setApartmentWasteItemId(apartmentWasteItems[0]?.id ?? 0);

    setApartmentDay("MONDAY");

    setApartmentStartTime("18:00");

    setApartmentEndTime("21:00");

    setApartmentAlwaysAvailable(false);

    setApartmentModalOpen(true);
  }

  function openApartmentEdit(schedule: ApartmentSchedule) {
    setEditingApartmentSchedule(schedule);

    setApartmentWasteItemId(schedule.wasteItemId);

    setApartmentDay(schedule.dayOfWeek);

    setApartmentStartTime(schedule.startTime ?? "18:00");

    setApartmentEndTime(schedule.endTime ?? "21:00");

    setApartmentAlwaysAvailable(schedule.alwaysAvailable);

    setApartmentModalOpen(true);
  }

  async function saveApartment() {
    if (selectedApartmentId === null || apartmentWasteItemId === 0) {
      return;
    }

    try {
      await adminApi.saveApartmentSchedule({
        id: editingApartmentSchedule?.id,
        apartmentId: selectedApartmentId,
        wasteItemId: apartmentWasteItemId,
        dayOfWeek: apartmentDay,
        startTime: apartmentStartTime,
        endTime: apartmentEndTime,
        alwaysAvailable: apartmentAlwaysAvailable,
      });

      setApartmentModalOpen(false);

      await loadApartmentSchedules(selectedApartmentId);
    } catch (error) {
      window.alert(
        error instanceof Error
          ? error.message
          : "공동주택 일정 저장에 실패했습니다.",
      );
    }
  }

  async function removeApartment(schedule: ApartmentSchedule) {
    if (
      !window.confirm(
        `${schedule.apartmentName}의 ${schedule.wasteItemName} 일정을 삭제할까요?`,
      )
    ) {
      return;
    }

    try {
      await adminApi.deleteApartmentSchedule(schedule.id);

      if (selectedApartmentId !== null) {
        await loadApartmentSchedules(selectedApartmentId);
      }
    } catch (error) {
      window.alert(
        error instanceof Error
          ? error.message
          : "공동주택 일정 삭제에 실패했습니다.",
      );
    }
  }

  return (
    <>
      <PageHeader
        title="배출 일정 관리"
        subtitle="일반주택 수거구역 일정과 공동주택 일정을 분리해 관리합니다."
      />

      <div className="tabs schedule-tabs">
        <button
          className={tab === "GENERAL" ? "active" : ""}
          onClick={() => setTab("GENERAL")}
        >
          일반주택 일정
        </button>

        <button
          className={tab === "APARTMENT" ? "active" : ""}
          onClick={() => setTab("APARTMENT")}
        >
          공동주택 일정
        </button>
      </div>

      {tab === "GENERAL" ? (
        <>
          <div className="notice">
            <b>이제 실제 지역 단위로 묶어서 표시합니다.</b>
            <span>
              일정이 하나도 없는 지역도 미등록 상태로 함께 보여서 누락 지역을
              바로 확인할 수 있습니다.
            </span>
          </div>

          <section className="schedule-filter-panel">
            <div className="schedule-filter-grid">
              <label className="schedule-search-field">
                <span>지역 검색</span>
                <input
                  className="search"
                  placeholder="시/도, 시/군/구, 수거구역, 대상지역"
                  value={generalQueryInput}
                  onChange={(event) => setGeneralQueryInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      setGeneralPage(1);
                      setGeneralQuery(generalQueryInput.trim());
                    }
                  }}
                />
              </label>

              <label>
                <span>출처</span>
                <select
                  value={generalSource}
                  onChange={(event) => {
                    setGeneralPage(1);
                    setGeneralSource(
                      event.target.value as
                        | ""
                        | "MOIS_HOUSEHOLD_WASTE"
                        | "MANUAL",
                    );
                  }}
                >
                  <option value="">전체 출처</option>
                  <option value="MOIS_HOUSEHOLD_WASTE">공공데이터</option>
                  <option value="MANUAL">관리자 직접 등록</option>
                </select>
              </label>

              <label>
                <span>수거구역 상태</span>
                <select
                  value={generalActive}
                  onChange={(event) => {
                    setGeneralPage(1);
                    setGeneralActive(
                      event.target.value as "" | "true" | "false",
                    );
                  }}
                >
                  <option value="">전체 상태</option>
                  <option value="true">활성</option>
                  <option value="false">비활성</option>
                </select>
              </label>

              <div className="schedule-filter-actions">
                <button
                  className="btn primary"
                  onClick={() => {
                    setGeneralPage(1);
                    setGeneralQuery(generalQueryInput.trim());
                  }}
                >
                  검색
                </button>

                <button
                  className="btn"
                  onClick={() => {
                    setGeneralQueryInput("");
                    setGeneralQuery("");
                    setGeneralSource("");
                    setGeneralActive("");
                    setGeneralPage(1);
                  }}
                >
                  초기화
                </button>
              </div>
            </div>

            <div className="area-result-summary">
              <span>
                전체 지역 <b>{generalTotalElements.toLocaleString()}</b>개
              </span>

              <span>페이지당 지역 {PAGE_SIZE}개</span>
            </div>
          </section>

          {generalError && <div className="error-box">{generalError}</div>}

          {generalLoading ? (
            <Loading />
          ) : coverageGroups.length === 0 ? (
            <Empty text="조건에 맞는 수거구역이 없습니다." />
          ) : (
            <>
              <div className="schedule-group-guide">
                <div>
                  <b>같은 지역은 한 줄로 표시합니다.</b>
                  <span>
                    배출 방법·장소 등 세부 규칙이 다른 경우에는 "세부 일정
                    보기"에서 각각 확인할 수 있습니다.
                  </span>
                </div>
                <span className="schedule-group-count">
                  현재 페이지 지역 {generalGroups.length}개
                </span>
              </div>

              <TableWrap>
                <table className="schedule-coverage-table grouped">
                  <thead>
                    <tr>
                      <th>수거지역</th>
                      <th>행정구역</th>
                      <th>일정 등록 현황</th>
                      <th>상태</th>
                      <th />
                    </tr>
                  </thead>

                  <tbody>
                    {generalGroups.map((group) => {
                      const groupKey = String(
                        group.representativeCollectionAreaId,
                      );

                      const expanded = expandedGeneralGroups.has(groupKey);

                      return (
                        <Fragment key={groupKey}>
                          <tr className="schedule-group-row">
                            <td>
                              <b>{group.collectionAreaName}</b>

                              <small>
                                {group.targetAreaName || "대상지역 정보 없음"}
                              </small>

                              <small>
                                {group.areaSourceType === "MANUAL"
                                  ? "관리자 직접 등록"
                                  : "공공데이터"}
                                {" · "}
                                {group.active ? "활성" : "비활성"}
                              </small>
                            </td>

                            <td>
                              {group.sido}
                              <br />
                              {group.district}
                            </td>

                            <td>
                              <div className="schedule-preview-stack">
                                {group.wasteTypeCoverage.map((coverage) => (
                                  <div
                                    className="schedule-preview-line"
                                    key={coverage.wasteType}
                                  >
                                    <b>{wasteTypeLabels[coverage.wasteType]}</b>

                                    <span>
                                      {coverage.registeredAreaCount}/
                                      {coverage.supportedAreaCount} 등록
                                    </span>

                                    {coverage.missingAreaCount > 0 ? (
                                      <span className="schedule-incomplete">
                                        미등록 {coverage.missingAreaCount}개
                                      </span>
                                    ) : (
                                      <span className="schedule-complete">
                                        완료
                                      </span>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </td>

                            <td>
                              {group.allSchedulesRegistered ? (
                                <span className="schedule-complete">
                                  일정 등록 완료
                                </span>
                              ) : (
                                <span className="schedule-incomplete">
                                  미등록 일정 있음
                                </span>
                              )}

                              <small className="schedule-group-meta">
                                세부 규칙{" "}
                                {group.collectionAreaCount.toLocaleString()}개
                              </small>
                            </td>

                            <td className="right">
                              <button
                                className={`btn small ${
                                  expanded ? "" : "primary"
                                }`}
                                onClick={() => toggleGeneralGroup(groupKey)}
                              >
                                {expanded
                                  ? "접기"
                                  : `상세 보기 (${group.collectionAreaCount})`}
                              </button>
                            </td>
                          </tr>

                          {expanded && (
                            <tr className="schedule-group-detail-row">
                              <td colSpan={5}>
                                <div className="schedule-pattern-grid">
                                  {group.areas.map((area, index) => (
                                    <section
                                      className="schedule-pattern-card"
                                      key={area.collectionAreaId}
                                    >
                                      <header>
                                        <div>
                                          <b>세부 규칙 {index + 1}</b>

                                          <small>
                                            CollectionArea #
                                            {area.collectionAreaId}
                                            {area.externalManagementNumber
                                              ? ` · 관리번호 ${area.externalManagementNumber}`
                                              : ""}
                                          </small>
                                        </div>

                                        {area.missingWasteTypes.length === 0 ? (
                                          <span className="schedule-complete">
                                            모두 등록
                                          </span>
                                        ) : (
                                          <span className="schedule-incomplete">
                                            일부 미등록
                                          </span>
                                        )}
                                      </header>

                                      {area.schedules.length === 0 ? (
                                        <span className="schedule-none">
                                          등록된 일정 없음
                                        </span>
                                      ) : (
                                        <div className="schedule-stack">
                                          {area.schedules.map((schedule) => (
                                            <div
                                              className="schedule-row-card"
                                              key={schedule.id}
                                            >
                                              <div>
                                                <b>
                                                  {
                                                    wasteTypeLabels[
                                                      schedule.wasteType
                                                    ]
                                                  }
                                                </b>

                                                <span>
                                                  {schedule.day} ·{" "}
                                                  {schedule.time}
                                                </span>

                                                <small>
                                                  {schedule.sourceType ===
                                                  "PUBLIC_DATA"
                                                    ? "공공데이터"
                                                    : "관리자 확정"}
                                                </small>

                                                {schedule.note && (
                                                  <small className="schedule-detail-note">
                                                    {schedule.note}
                                                  </small>
                                                )}
                                              </div>

                                              <div className="schedule-row-actions">
                                                <button
                                                  className="btn small"
                                                  onClick={() =>
                                                    openGeneralEdit(
                                                      area,
                                                      schedule,
                                                    )
                                                  }
                                                >
                                                  수정
                                                </button>

                                                <button
                                                  className="btn small danger"
                                                  onClick={() =>
                                                    removeGeneral(schedule)
                                                  }
                                                >
                                                  삭제
                                                </button>
                                              </div>
                                            </div>
                                          ))}
                                        </div>
                                      )}

                                      <footer className="schedule-pattern-footer">
                                        <div className="missing-stack">
                                          {area.missingWasteTypes.map(
                                            (wasteType) => (
                                              <button
                                                key={wasteType}
                                                className="missing-chip"
                                                onClick={() =>
                                                  openGeneralCreate(
                                                    area,
                                                    wasteType,
                                                  )
                                                }
                                              >
                                                + {wasteTypeLabels[wasteType]}
                                              </button>
                                            ),
                                          )}
                                        </div>

                                        <button
                                          className="btn small primary"
                                          disabled={
                                            area.missingWasteTypes.length === 0
                                          }
                                          onClick={() =>
                                            openGeneralCreate(area)
                                          }
                                        >
                                          일정 추가
                                        </button>
                                      </footer>
                                    </section>
                                  ))}
                                </div>
                              </td>
                            </tr>
                          )}
                        </Fragment>
                      );
                    })}
                  </tbody>
                </table>
              </TableWrap>

              <div className="pagination-bar">
                <div className="pagination-summary">
                  {generalTotalPages > 0
                    ? `${generalPage} / ${generalTotalPages} 페이지`
                    : "0 페이지"}
                </div>

                <div className="pagination">
                  <button
                    className="page-btn"
                    disabled={generalPage <= 1}
                    onClick={() =>
                      setGeneralPage((value) => Math.max(1, value - 1))
                    }
                  >
                    ‹ 이전
                  </button>

                  {generalPages.map((pageNumber) => (
                    <button
                      key={pageNumber}
                      className={`page-btn ${
                        pageNumber === generalPage ? "active" : ""
                      }`}
                      onClick={() => setGeneralPage(pageNumber)}
                    >
                      {pageNumber}
                    </button>
                  ))}

                  <button
                    className="page-btn"
                    disabled={
                      generalTotalPages === 0 ||
                      generalPage >= generalTotalPages
                    }
                    onClick={() =>
                      setGeneralPage((value) =>
                        Math.min(generalTotalPages, value + 1),
                      )
                    }
                  >
                    다음 ›
                  </button>
                </div>
              </div>
            </>
          )}

          <Modal
            open={generalModalOpen}
            title={editingGeneral ? "일반주택 일정 수정" : "일반주택 일정 추가"}
            onClose={() => setGeneralModalOpen(false)}
            footer={
              <>
                <button
                  className="btn"
                  onClick={() => setGeneralModalOpen(false)}
                >
                  취소
                </button>

                <button
                  className="btn primary"
                  disabled={!generalDays.trim()}
                  onClick={saveGeneral}
                >
                  저장
                </button>
              </>
            }
          >
            <div className="form-grid">
              <label>
                <span>수거구역</span>
                <input value={generalArea?.collectionAreaName ?? ""} disabled />
              </label>

              <label>
                <span>폐기물 종류</span>
                <select
                  value={generalWasteType}
                  disabled={Boolean(editingGeneral)}
                  onChange={(event) =>
                    setGeneralWasteType(
                      event.target.value as CollectionWasteType,
                    )
                  }
                >
                  {(generalArea?.supportedWasteTypes ?? []).map((wasteType) => (
                    <option key={wasteType} value={wasteType}>
                      {wasteTypeLabels[wasteType]}
                    </option>
                  ))}
                </select>
              </label>

              <label className="full">
                <span>배출 요일</span>
                <input
                  value={generalDays}
                  onChange={(event) => setGeneralDays(event.target.value)}
                  placeholder="예: 월, 수, 금 / 매주 화요일"
                />
              </label>

              <label>
                <span>시작 시간</span>
                <input
                  type="time"
                  value={generalStartTime}
                  onChange={(event) => setGeneralStartTime(event.target.value)}
                />
              </label>

              <label>
                <span>종료 시간</span>
                <input
                  type="time"
                  value={generalEndTime}
                  onChange={(event) => setGeneralEndTime(event.target.value)}
                />
              </label>

              <p className="form-help full">
                시간이 정해져 있지 않다면 시작·종료 시간을 둘 다 비워둘 수
                있습니다.
              </p>
            </div>
          </Modal>
        </>
      ) : (
        <>
          <div className="notice">
            <b>공동주택은 건물별로 일정을 관리합니다.</b>
            <span>
              승인된 공동주택을 선택하면 해당 아파트·오피스텔의 품목별 일정을
              조회합니다.
            </span>
          </div>

          <section className="apartment-schedule-selector">
            <label>
              <span>공동주택 검색</span>
              <input
                className="search"
                value={apartmentKeyword}
                onChange={(event) => setApartmentKeyword(event.target.value)}
                placeholder="공동주택명 또는 주소"
              />
            </label>

            <label>
              <span>공동주택 선택</span>
              <select
                value={selectedApartmentId ?? ""}
                onChange={(event) =>
                  setSelectedApartmentId(
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              >
                {filteredApartments.length === 0 && (
                  <option value="">승인된 공동주택 없음</option>
                )}

                {filteredApartments.map((apartment) => (
                  <option key={apartment.id} value={apartment.id}>
                    {apartment.name} · {apartment.address}
                  </option>
                ))}
              </select>
            </label>

            <button
              className="btn primary"
              disabled={
                selectedApartmentId === null || apartmentWasteItems.length === 0
              }
              onClick={openApartmentCreate}
            >
              + 공동주택 일정 추가
            </button>
          </section>

          {selectedApartment && (
            <div className="selected-apartment-card">
              <div>
                <small>선택된 공동주택</small>
                <b>{selectedApartment.name}</b>
                <span>{selectedApartment.address}</span>
              </div>

              <span className="mini-chip">
                등록 일정 {apartmentSchedules.length}개
              </span>
            </div>
          )}

          {apartmentError && <div className="error-box">{apartmentError}</div>}

          {apartmentLoading ? (
            <Loading />
          ) : selectedApartmentId === null ? (
            <Empty text="관리할 공동주택을 선택해주세요." />
          ) : apartmentSchedules.length === 0 ? (
            <Empty text="이 공동주택에 등록된 배출 일정이 없습니다." />
          ) : (
            <TableWrap>
              <table>
                <thead>
                  <tr>
                    <th>공동주택</th>
                    <th>품목</th>
                    <th>요일</th>
                    <th>시간</th>
                    <th>상시 배출</th>
                    <th>최근 수정</th>
                    <th />
                  </tr>
                </thead>

                <tbody>
                  {apartmentSchedules.map((schedule) => (
                    <tr key={schedule.id}>
                      <td>
                        <b>{schedule.apartmentName}</b>
                      </td>

                      <td>{schedule.wasteItemName}</td>

                      <td>
                        {dayLabels[schedule.dayOfWeek] ?? schedule.dayOfWeek}
                      </td>

                      <td>
                        {schedule.alwaysAvailable
                          ? "상시"
                          : `${schedule.startTime ?? "-"} ~ ${
                              schedule.endTime ?? "-"
                            }`}
                      </td>

                      <td>
                        <Badge
                          value={
                            schedule.alwaysAvailable ? "ACTIVE" : "INACTIVE"
                          }
                        />
                      </td>

                      <td>{schedule.updatedAt || "-"}</td>

                      <td className="right">
                        <div className="actions">
                          <button
                            className="btn small"
                            onClick={() => openApartmentEdit(schedule)}
                          >
                            수정
                          </button>

                          <button
                            className="btn small danger"
                            onClick={() => removeApartment(schedule)}
                          >
                            삭제
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableWrap>
          )}

          <Modal
            open={apartmentModalOpen}
            title={
              editingApartmentSchedule
                ? "공동주택 일정 수정"
                : "공동주택 일정 추가"
            }
            onClose={() => setApartmentModalOpen(false)}
            footer={
              <>
                <button
                  className="btn"
                  onClick={() => setApartmentModalOpen(false)}
                >
                  취소
                </button>

                <button
                  className="btn primary"
                  disabled={apartmentWasteItemId === 0}
                  onClick={saveApartment}
                >
                  저장
                </button>
              </>
            }
          >
            <div className="form-grid">
              <label className="full">
                <span>공동주택</span>
                <input value={selectedApartment?.name ?? ""} disabled />
              </label>

              <label>
                <span>폐기물 품목</span>
                <select
                  value={apartmentWasteItemId}
                  disabled={Boolean(editingApartmentSchedule)}
                  onChange={(event) =>
                    setApartmentWasteItemId(Number(event.target.value))
                  }
                >
                  {apartmentWasteItems.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>요일</span>
                <select
                  value={apartmentDay}
                  onChange={(event) => setApartmentDay(event.target.value)}
                >
                  {Object.entries(dayLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="check full">
                <input
                  type="checkbox"
                  checked={apartmentAlwaysAvailable}
                  onChange={(event) =>
                    setApartmentAlwaysAvailable(event.target.checked)
                  }
                />
                상시 배출 가능한 품목입니다.
              </label>

              {!apartmentAlwaysAvailable && (
                <>
                  <label>
                    <span>시작 시간</span>
                    <input
                      type="time"
                      value={apartmentStartTime}
                      onChange={(event) =>
                        setApartmentStartTime(event.target.value)
                      }
                    />
                  </label>

                  <label>
                    <span>종료 시간</span>
                    <input
                      type="time"
                      value={apartmentEndTime}
                      onChange={(event) =>
                        setApartmentEndTime(event.target.value)
                      }
                    />
                  </label>
                </>
              )}
            </div>
          </Modal>
        </>
      )}
    </>
  );
}

function WasteItemsPage() {
  const [items, setItems] = useState<WasteItem[]>([]);
  const [q, setQ] = useState("");
  const [editing, setEditing] = useState<WasteItem | null>(null);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [category, setCategory] = useState("폐전지류");
  const [aliases, setAliases] = useState("");
  const [ai, setAi] = useState(false);
  const load = async () => setItems(await adminApi.wasteItems(q));
  useEffect(() => {
    load();
  }, []);
  function show(x?: WasteItem) {
    setEditing(x ?? null);
    setName(x?.name ?? "");
    setCategory(x?.category ?? "폐전지류");
    setAliases(x?.aliases.join(", ") ?? "");
    setAi(x?.aiSupported ?? false);
    setOpen(true);
  }
  async function save() {
    await adminApi.saveWasteItem({
      id: editing?.id,
      name,
      category,
      aliases: aliases
        .split(",")
        .map((x) => x.trim())
        .filter(Boolean),
      aiSupported: ai,
      active: true,
    });
    setOpen(false);
    await load();
  }
  return (
    <>
      <PageHeader
        title="폐기물 품목 관리"
        subtitle="AI 인식 범위와 별개로 사용자가 검색할 수 있는 전체 폐기물 품목을 관리합니다."
        action={
          <button className="btn primary" onClick={() => show()}>
            + 품목 추가
          </button>
        }
      />
      <div className="notice">
        <b>AI 미지원 품목도 서비스에는 등록할 수 있어요.</b>
        <span>
          폐건전지, 전선, 마우스, 충전기, 키보드 등은 직접 검색 fallback으로
          정확한 가이드를 제공할 수 있습니다.
        </span>
      </div>
      <div className="toolbar">
        <input
          className="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="품목명, 카테고리, 별칭 검색"
        />
        <button className="btn primary" onClick={load}>
          검색
        </button>
      </div>
      <TableWrap>
        <table>
          <thead>
            <tr>
              <th>품목</th>
              <th>카테고리</th>
              <th>검색 별칭</th>
              <th>AI 인식</th>
              <th>상태</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((x) => (
              <tr key={x.id}>
                <td>
                  <b>{x.name}</b>
                </td>
                <td>{x.category}</td>
                <td>{x.aliases.join(", ") || "-"}</td>
                <td>
                  <span className={x.aiSupported ? "ai-ok" : "muted"}>
                    {x.aiSupported ? "지원" : "검색 전용"}
                  </span>
                </td>
                <td>
                  <Badge value={x.active ? "ACTIVE" : "INACTIVE"} />
                </td>
                <td className="right">
                  <button className="btn small" onClick={() => show(x)}>
                    수정
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableWrap>
      <Modal
        open={open}
        title={editing ? "폐기물 품목 수정" : "폐기물 품목 추가"}
        onClose={() => setOpen(false)}
        footer={
          <>
            <button className="btn" onClick={() => setOpen(false)}>
              취소
            </button>
            <button
              className="btn primary"
              onClick={save}
              disabled={!name.trim()}
            >
              저장
            </button>
          </>
        }
      >
        <div className="form-grid">
          <label>
            <span>품목명</span>
            <input value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            <span>카테고리</span>
            <input
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />
          </label>
          <label className="full">
            <span>검색 별칭</span>
            <input
              value={aliases}
              onChange={(e) => setAliases(e.target.value)}
              placeholder="건전지, 배터리, AA배터리"
            />
          </label>
          <label className="check full">
            <input type="checkbox" checked={ai} disabled /> AI 지원 여부는
            배포된 모델 클래스 기준으로 자동 표시됩니다.
          </label>
        </div>
      </Modal>
    </>
  );
}

function GuidesPage() {
  const [items, setItems] = useState<Guide[]>([]);
  const [editing, setEditing] = useState<Guide | null>(null);
  const [open, setOpen] = useState(false);
  const [wasteItem, setWasteItem] = useState("");
  const [summary, setSummary] = useState("");
  const [method, setMethod] = useState("");
  const [caution, setCaution] = useState("");
  const [checks, setChecks] = useState("");
  const load = async () => setItems(await adminApi.guides());
  useEffect(() => {
    load();
  }, []);
  function show(x?: Guide) {
    setEditing(x ?? null);
    setWasteItem(x?.wasteItem ?? "");
    setSummary(x?.summary ?? "");
    setMethod(x?.method ?? "");
    setCaution(x?.caution ?? "");
    setChecks(x?.checks.join("\n") ?? "");
    setOpen(true);
  }
  async function save() {
    await adminApi.saveGuide({
      id: editing?.id,
      wasteItem,
      summary,
      method,
      caution,
      checks: checks
        .split("\n")
        .map((x) => x.trim())
        .filter(Boolean),
    });
    setOpen(false);
    await load();
  }
  return (
    <>
      <PageHeader
        title="분리배출 가이드 관리"
        subtitle="품목별 배출 방법, 주의사항, 사용자 체크리스트를 관리합니다."
        action={
          <button className="btn primary" onClick={() => show()}>
            + 가이드 추가
          </button>
        }
      />
      <div className="guide-list">
        {items.map((x) => (
          <article className="guide" key={x.id}>
            <div className="guide-head">
              <div>
                <small>{x.wasteItem}</small>
                <h3>{x.summary}</h3>
              </div>
              <button className="btn small" onClick={() => show(x)}>
                수정
              </button>
            </div>
            <div className="guide-cols">
              <div>
                <b>배출 방법</b>
                <p>{x.method}</p>
              </div>
              <div>
                <b>주의사항</b>
                <p>{x.caution}</p>
              </div>
            </div>
            <div className="chips">
              {x.checks.map((c) => (
                <span key={c}>{c}</span>
              ))}
            </div>
          </article>
        ))}
      </div>
      <Modal
        open={open}
        title={editing ? "분리배출 가이드 수정" : "분리배출 가이드 추가"}
        onClose={() => setOpen(false)}
        footer={
          <>
            <button className="btn" onClick={() => setOpen(false)}>
              취소
            </button>
            <button
              className="btn primary"
              onClick={save}
              disabled={!wasteItem.trim() || !summary.trim()}
            >
              저장
            </button>
          </>
        }
      >
        <div className="form-grid">
          <label>
            <span>품목</span>
            <input
              value={wasteItem}
              onChange={(e) => setWasteItem(e.target.value)}
            />
          </label>
          <label>
            <span>요약</span>
            <input
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
            />
          </label>
          <label className="full">
            <span>배출 방법</span>
            <textarea
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              rows={4}
            />
          </label>
          <label className="full">
            <span>주의사항</span>
            <textarea
              value={caution}
              onChange={(e) => setCaution(e.target.value)}
              rows={3}
            />
          </label>
          <label className="full">
            <span>체크리스트 (한 줄에 하나)</span>
            <textarea
              value={checks}
              onChange={(e) => setChecks(e.target.value)}
              rows={5}
            />
          </label>
        </div>
      </Modal>
    </>
  );
}

function AiPage() {
  const [items, setItems] = useState<AiCorrection[]>([]);
  const [filter, setFilter] = useState<ReviewStatus | "ALL">("PENDING");
  const [selected, setSelected] = useState<AiCorrection | null>(null);
  const [memo, setMemo] = useState("");
  const load = async () => setItems(await adminApi.corrections(filter));
  useEffect(() => {
    load();
  }, [filter]);
  async function review(ok: boolean) {
    if (!selected) return;
    await adminApi.reviewCorrection(selected.id, ok, memo);
    setSelected(null);
    setMemo("");
    await load();
  }

  return (
    <>
      <PageHeader
        title="AI 사용자 정정 검수"
        subtitle="AI 원본 예측과 사용자 수정값을 비교하고 재학습 후보를 승인합니다."
      />
      <div className="notice">
        <b>사용자 정정값을 바로 학습하지 않습니다.</b>
        <span>
          PENDING → 관리자 검수 → APPROVED / REJECTED. 승인된 데이터만 향후
          재학습 후보로 사용합니다.
        </span>
      </div>
      <div className="tabs">
        {(["PENDING", "APPROVED", "REJECTED", "ALL"] as const).map((x) => (
          <button
            className={filter === x ? "active" : ""}
            onClick={() => setFilter(x)}
            key={x}
          >
            {x === "PENDING"
              ? "검수 대기"
              : x === "APPROVED"
                ? "승인"
                : x === "REJECTED"
                  ? "거절"
                  : "전체"}
          </button>
        ))}
      </div>
      {!items.length ? (
        <Empty text="현재 필터에 해당하는 정정 데이터가 없습니다." />
      ) : (
        <TableWrap>
          <table>
            <thead>
              <tr>
                <th>ImageLog</th>
                <th>사용자</th>
                <th>AI 원본</th>
                <th>신뢰도</th>
                <th>사용자 정정</th>
                <th>상태</th>
                <th>정정 시각</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((x) => (
                <tr key={x.id}>
                  <td>#{x.imageLogId}</td>
                  <td>{x.userEmail}</td>
                  <td>
                    <b>{x.aiItem}</b>
                  </td>
                  <td>{(x.aiConfidence * 100).toFixed(1)}%</td>
                  <td>
                    <b className="ai-ok">{x.correctedItem}</b>
                  </td>
                  <td>
                    <Badge value={x.status} />
                  </td>
                  <td>{x.correctedAt}</td>
                  <td className="right">
                    <button
                      className="btn small"
                      onClick={() => {
                        setSelected(x);
                        setMemo(x.memo ?? "");
                      }}
                    >
                      {x.status === "PENDING" ? "검수하기" : "상세"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableWrap>
      )}
      <Modal
        open={!!selected}
        title={`AI 정정 검수 · ImageLog #${selected?.imageLogId ?? ""}`}
        onClose={() => setSelected(null)}
        footer={
          selected?.status === "PENDING" ? (
            <>
              <button className="btn danger" onClick={() => review(false)}>
                거절
              </button>
              <button className="btn primary" onClick={() => review(true)}>
                승인
              </button>
            </>
          ) : (
            <button className="btn" onClick={() => setSelected(null)}>
              닫기
            </button>
          )
        }
      >
        {selected && (
          <>
            <div className="compare">
              <div>
                <span>AI 원본</span>
                <b>{selected.aiItem}</b>
                <small>{(selected.aiConfidence * 100).toFixed(1)}%</small>
              </div>
              <strong>→</strong>
              <div className="correct">
                <span>사용자 정정</span>
                <b>{selected.correctedItem}</b>
                <small>관리자 확인 필요</small>
              </div>
            </div>
            <label>
              <span>검수 메모</span>
              <textarea
                rows={4}
                disabled={selected.status !== "PENDING"}
                value={memo}
                onChange={(e) => setMemo(e.target.value)}
                placeholder="승인/거절 사유 또는 재학습 참고사항"
              />
            </label>
          </>
        )}
      </Modal>
    </>
  );
}

function PublicDataPage() {
  const [items, setItems] = useState<SyncLog[]>([]);
  const [syncing, setSyncing] = useState(false);
  const load = async () => setItems(await adminApi.syncLogs());
  useEffect(() => {
    load();
  }, []);
  async function sync() {
    setSyncing(true);
    await adminApi.syncPublicData();
    await load();
    setSyncing(false);
  }
  return (
    <>
      <PageHeader
        title="공공데이터 동기화"
        subtitle="생활폐기물 배출정보와 수거구역 데이터를 수동 동기화하고 결과를 확인합니다."
        action={
          <button className="btn primary" onClick={sync} disabled={syncing}>
            {syncing ? "동기화 중..." : "↻ 지금 동기화"}
          </button>
        }
      />
      <div className="notice">
        <b>공공데이터 → SmartRecycle 내부 도메인 변환</b>
        <span>
          외부 원문을 그대로 노출하지 않고 수거구역, 일정, 품목 체계와 매핑해
          사용자에게 제공합니다.
        </span>
      </div>
      <TableWrap>
        <table>
          <thead>
            <tr>
              <th>데이터 소스</th>
              <th>상태</th>
              <th>추가</th>
              <th>갱신</th>
              <th>실패</th>
              <th>실행 시각</th>
            </tr>
          </thead>
          <tbody>
            {items.map((x) => (
              <tr key={x.id}>
                <td>
                  <b>{x.source}</b>
                </td>
                <td>
                  <Badge value={x.status} />
                </td>
                <td>{x.inserted}</td>
                <td>{x.updated}</td>
                <td>{x.failed}</td>
                <td>{x.at}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableWrap>
    </>
  );
}

function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([]);
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [target, setTarget] = useState("전체 사용자");
  const [busy, setBusy] = useState(false);

  const load = async () => setItems(await adminApi.notifications());
  useEffect(() => {
    load();
  }, []);

  async function sendNow() {
    setBusy(true);
    try {
      await adminApi.createNotification({ title, body, target });
      setOpen(false);
      setTitle("");
      setBody("");
      setTarget("전체 사용자");
      await load();
    } finally {
      setBusy(false);
    }
  }

  async function cancel(id: number) {
    if (
      !window.confirm(
        "이 알림을 발송 취소할까요? 발송 이력은 보존되고 사용자 알림함에서는 노출되지 않습니다.",
      )
    )
      return;
    await adminApi.cancelNotification(id);
    await load();
  }

  return (
    <>
      <PageHeader
        title="알림 관리"
        subtitle="운영 공지와 배출 안내를 대상 사용자에게 즉시 발송하고 발송 이력을 관리합니다."
        action={
          <button className="btn primary" onClick={() => setOpen(true)}>
            + 알림 발송
          </button>
        }
      />
      <div className="notice">
        <b>현재는 인앱 알림 즉시 발송을 지원합니다.</b>
        <span>
          예약 발송과 휴대폰 시스템 푸시(FCM)는 별도 기능이며, 현재 관리자
          화면에서는 제공하지 않습니다.
        </span>
      </div>
      {!items.length ? (
        <Empty text="발송된 알림이 없습니다." />
      ) : (
        <TableWrap>
          <table>
            <thead>
              <tr>
                <th>제목</th>
                <th>대상</th>
                <th>상태</th>
                <th>발송 시각</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((x) => (
                <tr key={x.id}>
                  <td>
                    <b>{x.title}</b>
                    <small>{x.body}</small>
                  </td>
                  <td>{x.target}</td>
                  <td>
                    <Badge value={x.status} />
                  </td>
                  <td>{x.sentAt ?? "-"}</td>
                  <td className="right">
                    {x.status === "SENT" && (
                      <button
                        className="btn small danger"
                        onClick={() => cancel(x.id)}
                      >
                        발송 취소
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableWrap>
      )}
      <Modal
        open={open}
        title="알림 즉시 발송"
        onClose={() => !busy && setOpen(false)}
        footer={
          <>
            <button
              className="btn"
              onClick={() => setOpen(false)}
              disabled={busy}
            >
              취소
            </button>
            <button
              className="btn primary"
              onClick={sendNow}
              disabled={busy || !title.trim() || !body.trim()}
            >
              {busy ? "발송 중..." : "즉시 발송"}
            </button>
          </>
        }
      >
        <div className="form-grid">
          <label className="full">
            <span>제목</span>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={120}
            />
          </label>
          <label className="full">
            <span>내용</span>
            <textarea
              rows={5}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              maxLength={2000}
            />
          </label>
          <label className="full">
            <span>대상</span>
            <select value={target} onChange={(e) => setTarget(e.target.value)}>
              <option value="전체 사용자">전체 활성 사용자</option>
              <option value="알림 수신 동의 사용자">
                알림 수신 동의 사용자
              </option>
              <option value="공동주택 사용자">공동주택 사용자</option>
              <option value="일반주택 사용자">일반주택 사용자</option>
            </select>
          </label>
        </div>
      </Modal>
    </>
  );
}

function SettingsPage() {
  return (
    <>
      <PageHeader
        title="관리자 설정"
        subtitle="관리자 웹 실행 환경과 보안 설정 기준을 확인합니다."
      />
      <div className="settings">
        <article>
          <i>⇄</i>
          <div>
            <b>Backend API</b>
            <span>현재 연결 주소</span>
            <code>{CONFIG.apiBaseUrl}</code>
          </div>
        </article>
        <article>
          <i>◈</i>
          <div>
            <b>실행 모드</b>
            <span>관리자 API 연결 상태</span>
            <code>{CONFIG.useMocks ? "DEMO / MOCK" : "REAL API"}</code>
          </div>
        </article>
        <article>
          <i>⌘</i>
          <div>
            <b>JWT 보안</b>
            <span>Secret은 웹에 포함하지 않음</span>
            <code>Backend Secret Only</code>
          </div>
        </article>
      </div>
      <section className="panel security">
        <h2>보안 체크</h2>
        <ul>
          <li>
            JWT Secret, 공공데이터 API Key, Kakao Key는
            `application-secret.yml`에만 저장합니다.
          </li>
          <li>
            관리자 권한은 프론트 화면 숨김이 아니라 Spring Security에서 반드시
            재검증합니다.
          </li>
          <li>
            사용자 AI 정정값은 관리자 검수 후 승인 데이터만 재학습 후보로
            사용합니다.
          </li>
        </ul>
      </section>
    </>
  );
}
