import { describe, expect, it } from 'vitest';
import { createLatestRequestGuard } from './resourceRequestState';

describe('createLatestRequestGuard', () => {
  it('only accepts the latest request id', () => {
    const guard = createLatestRequestGuard();
    const first = guard.begin();
    const second = guard.begin();

    expect(guard.isCurrent(first)).toBe(false);
    expect(guard.isCurrent(second)).toBe(true);
  });

  it('prevents a stale failure from replacing newer success state', async () => {
    const guard = createLatestRequestGuard();
    const state = { value: '', error: '' };
    const stale = guard.begin();
    const current = guard.begin();

    if (guard.isCurrent(current)) state.value = '新请求成功';
    if (guard.isCurrent(stale)) state.error = '旧请求失败';

    expect(state).toEqual({ value: '新请求成功', error: '' });
  });
});
