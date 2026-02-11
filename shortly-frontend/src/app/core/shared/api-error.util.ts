import type {HttpErrorResponse} from '@angular/common/http';


export type ApiError = Readonly<Record<string, unknown>> & {
  detail?: string;
};

export class ApiErrorUtil {
  private static readonly DEFAULT_FALLBACK_DETAIL = 'Erstellen fehlgeschlagen.';

  /**
   * Hauptmethode für Angular HttpErrorResponse:
   * - Nimmt HttpErrorResponse oder unknown
   * - Parst ggf. String-Fehler zu unknown
   * - Liefert eine saubere Detail-Nachricht
   */
  static getDetailFromHttpError(
    err: unknown,
    fallback: string = ApiErrorUtil.DEFAULT_FALLBACK_DETAIL
  ): string {
    const httpErr = ApiErrorUtil.asHttpErrorResponse(err);
    const rawPayload = ApiErrorUtil.normalizeHttpErrorPayload(httpErr?.error);
    return ApiErrorUtil.extractDetail(rawPayload, fallback);
  }

  /**
   * JSON.parse mit Unknown-Rückgabe, nie any.
   * Gibt bei Parse-Fehler den Original-String zurück.
   */
  static safeJsonParseUnknown(input: string): unknown {
    try {
      return JSON.parse(input) as unknown;
    } catch {
      return input;
    }
  }

  /** Type Guard */
  static isApiError(error: unknown): error is ApiError {
    if (typeof error !== 'object' || error === null) return false;

    const candidate = error as Record<string, unknown>;
    return (
      !('detail' in candidate) ||
      typeof candidate["detail"] === 'string' ||
      typeof candidate["detail"] === 'undefined'
    );
  }

  /**
   * Extrahiert eine Fehlermeldung aus verschiedenartigen Payloads.
   * - ApiError.detail
   * - Plain-String Fehler
   * - Fallback-Text
   */
  static extractDetail(
    raw: unknown,
    fallback: string = ApiErrorUtil.DEFAULT_FALLBACK_DETAIL
  ): string {
    if (ApiErrorUtil.isApiError(raw) && typeof raw.detail === 'string') {
      return raw.detail;
    }
    if (typeof raw === 'string') {
      const message = raw.trim();
      if (message.length > 0) return message;
    }
    return fallback;
  }

  /**
   * Narrowing-Helfer: versucht, ein unknown auf HttpErrorResponse zu mappen,
   * ohne riskante any-Zugriffe.
   */
  private static asHttpErrorResponse(
    err: unknown
  ): Partial<HttpErrorResponse> | undefined {
    if (typeof err === 'object' && err !== null) {
      return err as Partial<HttpErrorResponse>;
    }
    return undefined;
  }

  /**
   * Normalisiert den error-Payload:
   * - Wenn string: versucht JSON zu parsen
   * - Sonst: unverändert zurück
   */
  private static normalizeHttpErrorPayload(errorPayload: unknown): unknown {
    if (typeof errorPayload === 'string') {
      return ApiErrorUtil.safeJsonParseUnknown(errorPayload);
    }
    return errorPayload;
  }
}
