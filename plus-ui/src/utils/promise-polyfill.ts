const PromiseCtor = Promise as PromiseConstructor & {
  try?: <T>(callback: (...args: any[]) => T | PromiseLike<T>, ...args: any[]) => Promise<T>;
};

if (typeof PromiseCtor.try !== 'function') {
  PromiseCtor.try = function promiseTry<T>(callback: (...args: any[]) => T | PromiseLike<T>, ...args: any[]): Promise<T> {
    return new Promise((resolve) => resolve(callback(...args)));
  };
}
