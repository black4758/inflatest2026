const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

function getAuthHeaders(extraHeaders = {}) {
  const token = localStorage.getItem('auth_token');
  const headers = { ...extraHeaders };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

export async function signup(username, password) {
  const res = await fetch(`${API_BASE}/api/auth/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.message || '회원가입에 실패했습니다.');
  }
  return data;
}

export async function login(username, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.message || '로그인에 실패했습니다.');
  }
  return data;
}

export async function fetchHealth() {
  const start = performance.now();
  try {
    const res = await fetch(`${API_BASE}/actuator/health`);
    const duration = Math.round(performance.now() - start);
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      return { status: 'DOWN', statusCode: res.status, latency: duration, data };
    }
    const data = await res.json();
    return { status: data.status || 'UP', statusCode: res.status, latency: duration, data };
  } catch (err) {
    const duration = Math.round(performance.now() - start);
    return { status: 'OFFLINE', error: err.message, latency: duration };
  }
}

export async function fetchPosts(keyword = '', sortBy = 'latest') {
  const params = new URLSearchParams();
  if (keyword) params.append('keyword', keyword);
  if (sortBy) params.append('sortBy', sortBy);
  const queryString = params.toString();
  const url = queryString ? `${API_BASE}/api/posts?${queryString}` : `${API_BASE}/api/posts`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`게시글 목록 조회 실패: ${res.status}`);
  }
  return res.json();
}

export async function fetchPostById(id) {
  const res = await fetch(`${API_BASE}/api/posts/${id}`);
  if (!res.ok) {
    throw new Error(`게시글 조회 실패: ${res.status}`);
  }
  return res.json();
}

export async function createPost(postData) {
  const res = await fetch(`${API_BASE}/api/posts`, {
    method: 'POST',
    headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(postData),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || (err.fields ? Object.values(err.fields).join(', ') : '게시글 작성 실패'));
  }
  return res.json();
}

export async function updatePost(id, postData) {
  const res = await fetch(`${API_BASE}/api/posts/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(postData),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || (err.fields ? Object.values(err.fields).join(', ') : '게시글 수정 실패'));
  }
  return res.json();
}

export async function deletePost(id) {
  const res = await fetch(`${API_BASE}/api/posts/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `게시글 삭제 실패: ${res.status}`);
  }
  return true;
}

export async function createComment(postId, content) {
  const res = await fetch(`${API_BASE}/api/posts/${postId}/comments`, {
    method: 'POST',
    headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ content }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || '댓글 작성 실패');
  }
  return res.json();
}

export async function deleteComment(postId, commentId) {
  const res = await fetch(`${API_BASE}/api/posts/${postId}/comments/${commentId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || '댓글 삭제 실패');
  }
  return true;
}
