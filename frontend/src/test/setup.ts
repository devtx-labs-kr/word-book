import '@testing-library/jest-dom';

// jsdom's Blob/File.text() does not resolve, so polyfill it via FileReader for tests.
// Production code uses the real browser File.text() (U5 import reads the picked file).
Blob.prototype.text = function (): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsText(this as Blob);
  });
};
