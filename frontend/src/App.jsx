import React, { useState, useEffect } from 'react';
import {
  Server,
  Activity,
  Plus,
  Search,
  Eye,
  Clock,
  User,
  X,
  Edit3,
  Trash2,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Database,
  Flame,
  MessageSquare,
  LogIn,
  LogOut,
  UserPlus,
  Send
} from 'lucide-react';
import {
  fetchPosts,
  fetchPostById,
  createPost,
  updatePost,
  deletePost,
  fetchHealth,
  login,
  signup,
  createComment,
  deleteComment
} from './api';

export default function App() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [sortBy, setSortBy] = useState('latest'); // 'latest' | 'views'
  const [health, setHealth] = useState({ status: 'CHECKING', latency: 0, data: null });
  const [selectedPost, setSelectedPost] = useState(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingPost, setEditingPost] = useState(null);
  const [isHealthModalOpen, setIsHealthModalOpen] = useState(false);
  const [toast, setToast] = useState(null);

  // Auth states
  const [currentUser, setCurrentUser] = useState(() => {
    const savedUser = localStorage.getItem('auth_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState('login'); // 'login' | 'signup'
  const [authForm, setAuthForm] = useState({ username: '', password: '' });

  // Post & Comment Form states
  const [formData, setFormData] = useState({ title: '', content: '' });
  const [commentText, setCommentText] = useState('');

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3500);
  };

  const loadHealth = async () => {
    const res = await fetchHealth();
    setHealth(res);
  };

  const loadPosts = async (keyword = searchKeyword, sort = sortBy) => {
    setLoading(true);
    try {
      const data = await fetchPosts(keyword, sort);
      setPosts(data);
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHealth();
    loadPosts('', 'latest');

    const interval = setInterval(() => {
      loadHealth();
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    loadPosts(searchKeyword, sortBy);
  };

  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    if (!authForm.username.trim() || !authForm.password.trim()) {
      showToast('아이디와 비밀번호를 모두 입력해주세요.', 'error');
      return;
    }
    try {
      const res = authMode === 'login'
        ? await login(authForm.username, authForm.password)
        : await signup(authForm.username, authForm.password);

      localStorage.setItem('auth_token', res.token);
      localStorage.setItem('auth_user', JSON.stringify({ userId: res.userId, username: res.username }));
      setCurrentUser({ userId: res.userId, username: res.username });
      setIsAuthModalOpen(false);
      setAuthForm({ username: '', password: '' });
      showToast(authMode === 'login' ? `${res.username}님 환영합니다!` : `${res.username}님 회원가입 및 로그인 완료!`);
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    setCurrentUser(null);
    showToast('로그아웃되었습니다.');
  };

  const openCreateModal = () => {
    if (!currentUser) {
      showToast('게시글을 작성하려면 먼저 로그인해주세요.', 'error');
      setAuthMode('login');
      setIsAuthModalOpen(true);
      return;
    }
    setFormData({ title: '', content: '' });
    setIsCreateOpen(true);
  };

  const openEditModal = (post, e) => {
    if (e) e.stopPropagation();
    setEditingPost(post);
    setFormData({ title: post.title, content: post.content });
    setIsCreateOpen(false);
  };

  const handleRowClick = async (id) => {
    try {
      const post = await fetchPostById(id);
      setSelectedPost(post);
      setCommentText('');
      setPosts((prev) =>
        prev.map((p) => (p.id === id ? { ...p, viewCount: p.viewCount + 1, commentCount: post.commentCount } : p))
      );
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.content.trim()) {
      showToast('제목과 내용을 모두 입력해주세요.', 'error');
      return;
    }

    try {
      if (editingPost) {
        const updated = await updatePost(editingPost.id, formData);
        showToast('게시글이 성공적으로 수정되었습니다.');
        setEditingPost(null);
        if (selectedPost && selectedPost.id === updated.id) {
          setSelectedPost(updated);
        }
      } else {
        await createPost(formData);
        showToast('새 게시글이 등록되었습니다.');
        setIsCreateOpen(false);
      }
      loadPosts(searchKeyword, sortBy);
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const handleDelete = async (id, e) => {
    if (e) e.stopPropagation();
    if (!window.confirm('정말 이 게시글을 삭제하시겠습니까?')) return;

    try {
      await deletePost(id);
      showToast('게시글이 삭제되었습니다.');
      if (selectedPost && selectedPost.id === id) {
        setSelectedPost(null);
      }
      loadPosts(searchKeyword, sortBy);
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const handleCommentSubmit = async (e) => {
    e.preventDefault();
    if (!currentUser) {
      showToast('댓글을 작성하려면 먼저 로그인해주세요.', 'error');
      setAuthMode('login');
      setIsAuthModalOpen(true);
      return;
    }
    if (!commentText.trim()) return;

    try {
      await createComment(selectedPost.id, commentText);
      setCommentText('');
      const refreshed = await fetchPostById(selectedPost.id);
      setSelectedPost(refreshed);
      loadPosts(searchKeyword, sortBy);
      showToast('댓글이 등록되었습니다.');
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const handleCommentDelete = async (commentId) => {
    if (!window.confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await deleteComment(selectedPost.id, commentId);
      const refreshed = await fetchPostById(selectedPost.id);
      setSelectedPost(refreshed);
      loadPosts(searchKeyword, sortBy);
      showToast('댓글이 삭제되었습니다.');
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleDateString('ko-KR', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="app-layout">
      {/* Header */}
      <header className="app-header">
        <div className="brand-wrapper">
          <div className="brand-icon">
            <Server size={26} />
          </div>
          <div>
            <h1 className="brand-title">Infra Test CRUD Board</h1>
            <p className="brand-subtitle">Spring Boot 4.x + JPA + MySQL + Grafana • Blue/Green CI/CD</p>
          </div>
        </div>

        <div className="header-actions">
          {/* Actuator Status Badge */}
          <div
            className={`health-badge ${health.status.toLowerCase()}`}
            onClick={() => setIsHealthModalOpen(true)}
            title="클릭하여 /actuator/health 상세 정보 보기"
          >
            <span className="pulse-dot"></span>
            <span>
              Actuator: {health.status === 'UP' ? '정상 (UP)' : health.status}
              {health.latency ? ` • ${health.latency}ms` : ''}
            </span>
          </div>

          {/* Auth User Status / Login Button */}
          {currentUser ? (
            <div className="user-auth-bar">
              <span className="user-badge">
                <User size={14} />
                <strong>{currentUser.username}</strong>님
              </span>
              <button className="btn btn-secondary btn-sm" onClick={handleLogout} title="로그아웃">
                <LogOut size={14} /> 로그아웃
              </button>
            </div>
          ) : (
            <button
              className="btn btn-primary btn-sm"
              onClick={() => {
                setAuthMode('login');
                setIsAuthModalOpen(true);
              }}
            >
              <LogIn size={14} /> 로그인 / 회원가입
            </button>
          )}

          <button
            className="btn btn-secondary btn-sm"
            onClick={() => {
              loadHealth();
              loadPosts(searchKeyword, sortBy);
            }}
            title="새로고침"
          >
            <RefreshCw size={14} />
          </button>
        </div>
      </header>

      {/* Controls Bar */}
      <div className="controls-bar">
        <form onSubmit={handleSearch} className="search-box">
          <Search size={18} color="var(--text-dim)" />
          <input
            type="text"
            placeholder="제목, 내용 또는 작성자 검색..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
          {searchKeyword && (
            <button
              type="button"
              className="modal-close"
              onClick={() => {
                setSearchKeyword('');
                loadPosts('', sortBy);
              }}
            >
              <X size={16} />
            </button>
          )}
        </form>

        {/* Sort Tab Group (최신순 / 조회수순) */}
        <div className="sort-tab-group">
          <button
            type="button"
            className={`sort-tab ${sortBy === 'latest' ? 'active' : ''}`}
            onClick={() => {
              setSortBy('latest');
              loadPosts(searchKeyword, 'latest');
            }}
          >
            <Clock size={14} />
            최신순
          </button>
          <button
            type="button"
            className={`sort-tab ${sortBy === 'views' ? 'active' : ''}`}
            onClick={() => {
              setSortBy('views');
              loadPosts(searchKeyword, 'views');
            }}
          >
            <Flame size={14} />
            조회수순
          </button>
        </div>

        <button className="btn btn-primary" onClick={openCreateModal}>
          <Plus size={18} /> 새 글 작성
        </button>
      </div>

      {/* Post Table Container */}
      <div className="post-list-wrapper">
        <div className="table-header">
          <div>ID</div>
          <div>제목</div>
          <div>작성자</div>
          <div
            className={`sortable-header ${sortBy === 'views' ? 'active' : ''}`}
            onClick={() => {
              const next = sortBy === 'views' ? 'latest' : 'views';
              setSortBy(next);
              loadPosts(searchKeyword, next);
            }}
            title="클릭하여 조회수 순 정렬"
          >
            조회수 {sortBy === 'views' ? '▼' : ''}
          </div>
          <div
            className={`sortable-header ${sortBy === 'latest' ? 'active' : ''}`}
            onClick={() => {
              setSortBy('latest');
              loadPosts(searchKeyword, 'latest');
            }}
            title="클릭하여 최신순 정렬"
          >
            작성일시 {sortBy === 'latest' ? '▼' : ''}
          </div>
        </div>

        {loading ? (
          <div className="empty-state">
            <RefreshCw size={32} className="spin empty-state-icon" />
            <p>데이터를 불러오는 중입니다...</p>
          </div>
        ) : posts.length === 0 ? (
          <div className="empty-state">
            <Database size={40} className="empty-state-icon" />
            <h3>등록된 게시글이 없습니다.</h3>
            <p style={{ marginTop: '0.5rem' }}>'새 글 작성' 버튼을 눌러 첫 번째 글을 등록해보세요.</p>
          </div>
        ) : (
          posts.map((post) => (
            <div
              key={post.id}
              className="table-row"
              onClick={() => handleRowClick(post.id)}
            >
              <div className="post-id">#{post.id}</div>
              <div className="post-title-text">
                {post.title}
                {post.commentCount > 0 && (
                  <span className="comment-count-badge">
                    <MessageSquare size={12} /> {post.commentCount}
                  </span>
                )}
              </div>
              <div className="post-author">{post.author}</div>
              <div className="post-views">
                <Eye size={13} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }} />
                {post.viewCount}
              </div>
              <div className="post-date">{formatDate(post.createdAt)}</div>
            </div>
          ))
        )}
      </div>

      {/* Post Detail + Comments Modal */}
      {selectedPost && (
        <div className="modal-overlay" onClick={() => setSelectedPost(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">{selectedPost.title}</h2>
              <button className="modal-close" onClick={() => setSelectedPost(null)}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body">
              <div className="post-detail-meta">
                <span><User size={14} style={{ display: 'inline', marginRight: 4 }} /> {selectedPost.author}</span>
                <span><Eye size={14} style={{ display: 'inline', marginRight: 4 }} /> 조회 {selectedPost.viewCount}</span>
                <span><MessageSquare size={14} style={{ display: 'inline', marginRight: 4 }} /> 댓글 {selectedPost.comments?.length || 0}</span>
                <span><Clock size={14} style={{ display: 'inline', marginRight: 4 }} /> {formatDate(selectedPost.createdAt)}</span>
              </div>

              <div className="post-detail-content">{selectedPost.content}</div>

              {/* Comments Section */}
              <div className="comments-section">
                <h3 className="comments-title">
                  <MessageSquare size={16} /> 댓글 ({selectedPost.comments?.length || 0})
                </h3>

                <div className="comment-list">
                  {(!selectedPost.comments || selectedPost.comments.length === 0) ? (
                    <p className="comment-empty">첫 번째 댓글을 남겨보세요!</p>
                  ) : (
                    selectedPost.comments.map((comment) => (
                      <div key={comment.id} className="comment-item">
                        <div className="comment-header">
                          <span className="comment-author">
                            <User size={12} /> {comment.author}
                          </span>
                          <div className="comment-meta">
                            <span>{formatDate(comment.createdAt)}</span>
                            {currentUser && currentUser.username === comment.author && (
                              <button
                                type="button"
                                className="comment-delete-btn"
                                onClick={() => handleCommentDelete(comment.id)}
                                title="내 댓글 삭제"
                              >
                                <Trash2 size={13} /> 삭제
                              </button>
                            )}
                          </div>
                        </div>
                        <div className="comment-body">{comment.content}</div>
                      </div>
                    ))
                  )}
                </div>

                {/* Comment Input Form */}
                <form onSubmit={handleCommentSubmit} className="comment-form">
                  <input
                    type="text"
                    className="form-input"
                    placeholder={
                      currentUser
                        ? `${currentUser.username}님으로 댓글을 입력하세요...`
                        : '로그인 후 댓글을 작성할 수 있습니다.'
                    }
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                  />
                  <button type="submit" className="btn btn-primary btn-sm" style={{ whiteSpace: 'nowrap' }}>
                    <Send size={14} /> 등록
                  </button>
                </form>
              </div>
            </div>

            <div className="modal-footer">
              {/* 본인 글일 때만 수정/삭제 버튼 표시 */}
              {currentUser && currentUser.username === selectedPost.author && (
                <>
                  <button
                    className="btn btn-secondary"
                    onClick={(e) => openEditModal(selectedPost, e)}
                  >
                    <Edit3 size={16} /> 수정
                  </button>
                  <button
                    className="btn btn-danger"
                    onClick={(e) => handleDelete(selectedPost.id, e)}
                  >
                    <Trash2 size={16} /> 삭제
                  </button>
                </>
              )}
              <button className="btn btn-secondary" onClick={() => setSelectedPost(null)}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create / Edit Modal */}
      {(isCreateOpen || editingPost) && (
        <div
          className="modal-overlay"
          onClick={() => {
            setIsCreateOpen(false);
            setEditingPost(null);
          }}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleFormSubmit}>
              <div className="modal-header">
                <h2 className="modal-title">{editingPost ? '게시글 수정' : '새 게시글 작성'}</h2>
                <button
                  type="button"
                  className="modal-close"
                  onClick={() => {
                    setIsCreateOpen(false);
                    setEditingPost(null);
                  }}
                >
                  <X size={20} />
                </button>
              </div>

              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">작성자</label>
                  <input
                    type="text"
                    className="form-input"
                    value={currentUser ? currentUser.username : ''}
                    disabled
                    style={{ opacity: 0.7, cursor: 'not-allowed' }}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">제목</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="게시글 제목을 입력하세요"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">내용</label>
                  <textarea
                    className="form-textarea"
                    placeholder="내용을 작성하세요..."
                    value={formData.content}
                    onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setIsCreateOpen(false);
                    setEditingPost(null);
                  }}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingPost ? '수정 완료' : '등록'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Login / Signup Modal */}
      {isAuthModalOpen && (
        <div className="modal-overlay" onClick={() => setIsAuthModalOpen(false)}>
          <div className="modal-content" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleAuthSubmit}>
              <div className="modal-header">
                <h2 className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  {authMode === 'login' ? <LogIn size={20} color="var(--primary)" /> : <UserPlus size={20} color="var(--primary)" />}
                  {authMode === 'login' ? '로그인' : '회원가입'}
                </h2>
                <button type="button" className="modal-close" onClick={() => setIsAuthModalOpen(false)}>
                  <X size={20} />
                </button>
              </div>

              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">아이디 (닉네임)</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="2~50자 아이디 또는 닉네임"
                    value={authForm.username}
                    onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">비밀번호</label>
                  <input
                    type="password"
                    className="form-input"
                    placeholder="비밀번호 (4자 이상)"
                    value={authForm.password}
                    onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                    required
                  />
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: '0.5rem' }}>
                  {authMode === 'login' ? (
                    <>
                      계정이 없으신가요?{' '}
                      <button
                        type="button"
                        className="auth-switch-btn"
                        onClick={() => setAuthMode('signup')}
                      >
                        회원가입하기
                      </button>
                    </>
                  ) : (
                    <>
                      이미 계정이 있으신가요?{' '}
                      <button
                        type="button"
                        className="auth-switch-btn"
                        onClick={() => setAuthMode('login')}
                      >
                        로그인하기
                      </button>
                    </>
                  )}
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setIsAuthModalOpen(false)}>
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  {authMode === 'login' ? '로그인' : '가입 및 로그인'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Actuator Health Detail Modal */}
      {isHealthModalOpen && (
        <div className="modal-overlay" onClick={() => setIsHealthModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Activity size={22} color="var(--primary)" />
                스프링 부트 Actuator 헬스 상태
              </h2>
              <button className="modal-close" onClick={() => setIsHealthModalOpen(false)}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body">
              <div style={{ marginBottom: '1rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                <div style={{ background: 'rgba(255,255,255,0.04)', padding: '0.75rem 1rem', borderRadius: 8, flex: 1 }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Actuator Status</div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 700, color: health.status === 'UP' ? 'var(--success)' : 'var(--danger)' }}>
                    {health.status}
                  </div>
                </div>
                <div style={{ background: 'rgba(255,255,255,0.04)', padding: '0.75rem 1rem', borderRadius: 8, flex: 1 }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Response Latency</div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--accent-cyan)' }}>
                    {health.latency} ms
                  </div>
                </div>
              </div>

              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
                엔드포인트: <code>/actuator/health</code> 응답 원문
              </p>

              <pre className="health-json-block">
                {JSON.stringify(health.data || health, null, 2)}
              </pre>
            </div>

            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  loadHealth();
                }}
              >
                <RefreshCw size={14} /> 다시 조회
              </button>
              <button className="btn btn-primary" onClick={() => setIsHealthModalOpen(false)}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className={`toast ${toast.type}`}>
          {toast.type === 'success' ? (
            <CheckCircle2 size={18} color="var(--success)" />
          ) : (
            <AlertCircle size={18} color="var(--danger)" />
          )}
          <span>{toast.message}</span>
        </div>
      )}
    </div>
  );
}
