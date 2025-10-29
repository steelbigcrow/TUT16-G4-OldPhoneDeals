export interface Review {
  _id?: string;
  reviewerId: string;
  reviewerName?: string;        // reviewer full name
  rating: number;        // 1–5
  comment: string;
  createdAt?: Date;
  isHidden?: boolean;
  isExpanded?: boolean;  // track comment expansion state
}
