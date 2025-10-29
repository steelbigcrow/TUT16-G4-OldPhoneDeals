import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.scss'],
})
export class PaginationComponent implements OnChanges {
  // total pages
  @Input() totalPages = 1;
  // current page number (1-based)
  @Input() currentPage = 1;
  // output new page number when page changes
  @Output() pageChange = new EventEmitter<number>();

  pages: number[] = [];

  ngOnChanges(changes: SimpleChanges) {
    // regenerate page number array [1, 2, ..., totalPages]
    this.pages = Array(this.totalPages)
      .fill(0)
      .map((_, i) => i + 1);
  }

  selectPage(page: number) {
    if (page === this.currentPage) {
      return;
    }
    this.pageChange.emit(page);
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.selectPage(this.currentPage - 1);
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.selectPage(this.currentPage + 1);
    }
  }
}
