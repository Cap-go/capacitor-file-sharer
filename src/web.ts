import { WebPlugin } from '@capacitor/core';

import type {
  FileSharerPlugin,
  PluginVersionResult,
  SaveFileOptions,
  SaveFileResult,
  ShareFileOptions,
} from './definitions';

const DEFAULT_CONTENT_TYPE = 'application/octet-stream';
const ERR_PARAM_NO_FILENAME = 'ERR_PARAM_NO_FILENAME';
const ERR_PARAM_NO_DATA = 'ERR_PARAM_NO_DATA';
const ERR_PARAM_DATA_INVALID = 'ERR_PARAM_DATA_INVALID';
const ERR_LOCAL_FILE_NOT_FOUND = 'ERR_LOCAL_FILE_NOT_FOUND';
const BASE64_CHUNK_LENGTH = 256 * 1024;

export class FileSharerWeb extends WebPlugin implements FileSharerPlugin {
  async share(options: ShareFileOptions): Promise<void> {
    await this.download(options);
  }

  async save(options: SaveFileOptions): Promise<SaveFileResult> {
    await this.download(options);
    return {};
  }

  async getPluginVersion(): Promise<PluginVersionResult> {
    return {
      version: '8.0.0',
    };
  }

  private async download(options: ShareFileOptions): Promise<void> {
    this.validateOptions(options);

    const blob = await this.createBlob(options);
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');

    anchor.href = objectUrl;
    anchor.download = options.filename;
    anchor.rel = 'noopener';
    anchor.style.display = 'none';

    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();

    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
  }

  private validateOptions(options: ShareFileOptions): void {
    if (!options.filename?.trim()) {
      throw new Error(ERR_PARAM_NO_FILENAME);
    }

    if (!options.base64Data?.trim() && !options.path?.trim()) {
      throw new Error(ERR_PARAM_NO_DATA);
    }
  }

  private async createBlob(options: ShareFileOptions): Promise<Blob> {
    const contentType = options.contentType || DEFAULT_CONTENT_TYPE;

    if (options.base64Data?.trim()) {
      return new Blob(decodeBase64ToBlobParts(options.base64Data), {
        type: contentType,
      });
    }

    try {
      const response = await fetch(options.path as string);
      if (!response.ok) {
        throw new Error(ERR_LOCAL_FILE_NOT_FOUND);
      }

      const blob = await response.blob();
      return blob.type === contentType ? blob : new Blob([blob], { type: contentType });
    } catch {
      throw new Error(ERR_LOCAL_FILE_NOT_FOUND);
    }
  }
}

function decodeBase64ToBlobParts(base64Data: string): ArrayBuffer[] {
  const payload = getBase64Payload(base64Data);
  const chunkLength = Math.max(4, Math.floor(BASE64_CHUNK_LENGTH / 4) * 4);
  const parts: ArrayBuffer[] = [];

  try {
    for (let offset = 0; offset < payload.length; offset += chunkLength) {
      const byteCharacters = atob(payload.slice(offset, offset + chunkLength));
      const buffer = new ArrayBuffer(byteCharacters.length);
      const bytes = new Uint8Array(buffer);

      for (let index = 0; index < byteCharacters.length; index++) {
        bytes[index] = byteCharacters.charCodeAt(index);
      }

      parts.push(buffer);
    }
  } catch {
    throw new Error(ERR_PARAM_DATA_INVALID);
  }

  return parts;
}

function getBase64Payload(base64Data: string): string {
  const trimmed = base64Data.trim();
  const commaIndex = trimmed.indexOf(',');

  if (commaIndex > -1 && trimmed.slice(0, commaIndex).toLowerCase().includes('base64')) {
    return trimmed.slice(commaIndex + 1).replace(/\s/g, '');
  }

  return trimmed.replace(/\s/g, '');
}
