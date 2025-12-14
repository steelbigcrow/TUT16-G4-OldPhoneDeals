export type FileUploadResponse = {
  fileName: string;
  fileUrl: string;
  originalName: string | null;
  size: number | null;
  contentType: string | null;
};

