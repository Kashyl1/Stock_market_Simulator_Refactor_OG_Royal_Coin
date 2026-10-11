import { IDLE_REFRESH_TICKS, shouldRefresh } from './batch-models';

const FIRST_TICK = 1;
const VISIBLE = false;
const HIDDEN = true;
const RUNNING = true;
const IDLE = false;

describe('shouldRefresh', () => {
  it('refreshes on every tick while a job runs', () => {
    expect(shouldRefresh(FIRST_TICK, RUNNING, VISIBLE)).toBe(true);
  });

  it('refreshes only every few ticks while nothing runs', () => {
    expect(shouldRefresh(FIRST_TICK, IDLE, VISIBLE)).toBe(false);
    expect(shouldRefresh(IDLE_REFRESH_TICKS, IDLE, VISIBLE)).toBe(true);
  });

  it('never refreshes a hidden page', () => {
    expect(shouldRefresh(IDLE_REFRESH_TICKS, RUNNING, HIDDEN)).toBe(false);
  });
});
