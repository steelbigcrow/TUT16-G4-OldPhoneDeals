import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Review } from '../../models/review.model';

@Component({
  selector: 'app-review-item',
  standalone: true,
  imports: [CommonModule],
  styleUrls: ['./review-item.component.scss'],
  templateUrl: './review-item.component.html'
})
export class ReviewItemComponent implements OnInit, OnChanges {
  @Input() review!: Review;
  @Input() sellerId!: string;        // product seller ID
  @Input() currentUserId!: string;   // current logged in user ID

  // when the hide/show button is clicked, notify the parent component the reviewId and the new hidden value
  @Output() visibilityChange = new EventEmitter<{ reviewId: string; hidden: boolean }>();

  isExpanded = false;
  isOwnerOrSeller = false;

  ngOnInit(): void {
    this.checkPermissions();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // if the related input properties change, check the permissions again
    if (changes['currentUserId'] || changes['sellerId'] || changes['review']) {
      this.checkPermissions();
    }
  }

  // check if the current user is the review author or the item seller
  private checkPermissions(): void {
    // check if the current user is logged in
    if (!this.currentUserId) {
      this.isOwnerOrSeller = false;
      return;
    }

    // check if the current user is the review author
    const isReviewer = this.currentUserId === this.review.reviewerId;

    // check if the current user is the product seller
    const isSeller = this.currentUserId === this.sellerId;

    // set the permission flag
    this.isOwnerOrSeller = isReviewer || isSeller;

    // debug information
    console.log('Review permissions check:', {
      reviewId: this.review._id,
      reviewerId: this.review.reviewerId,
      currentUserId: this.currentUserId,
      sellerId: this.sellerId,
      isReviewer,
      isSeller,
      isOwnerOrSeller: this.isOwnerOrSeller
    });
  }

  // toggle "expand/collapse"
  toggleShowMore(): void {
    this.isExpanded = !this.isExpanded;
  }

  // toggle visibility, and notify the parent component
  toggleVisibility(): void {
    // only allow the review author or the product seller to operate
    if (!this.isOwnerOrSeller) {
      console.warn('Unauthorized attempt to toggle review visibility');
      return;
    }

    // update the local state and notify the parent component
    const newHiddenState = !this.review.isHidden;
    this.visibilityChange.emit({
      reviewId: this.review._id!,
      hidden: newHiddenState
    });
  }
}
