type ApiEnvelope<T> = {
  success: boolean;
  status: number;
  data: T;
  error: string | null;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090';

export async function apiGet<T>(path: string): Promise<ApiEnvelope<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`);
  return parseResponse<T>(response);
}

export async function apiGetText(path: string): Promise<string> {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error('Health check failed');
  }

  return response.text();
}

async function parseResponse<T>(response: Response): Promise<ApiEnvelope<T>> {
  let payload: ApiEnvelope<T>;

  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    throw new Error('Invalid API response');
  }

  if (!response.ok || payload.success === false) {
    throw new Error(payload.error || 'API request failed');
  }

  return payload;
}
