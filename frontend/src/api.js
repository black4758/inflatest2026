const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

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

export async function fetchPosts(keyword = '') {
  const url = keyword ? `${API_BASE}/api/posts?keyword=${encodeURIComponent(keyword)}` : `${API_BASE}/api/posts`;
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
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(postData),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.fields ? Object.values(err.fields).join(', ') : '게시글 작성 실패');
  }
  return res.json();
}

export async function updatePost(id, postData) {
  const res = await fetch(`${API_BASE}/api/posts/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(postData),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.fields ? Object.values(err.fields).join(', ') : '게시글 수정 실패');
  }
  return res.json();
}

export async function deletePost(id) {
  const res = await fetch(`${API_BASE}/api/posts/${id}`, {
    method: 'DELETE',
  });
  if (!res.ok) {
    throw new Error(`게시글 삭제 실패: ${res.status}`);
  }
  return true;
}
