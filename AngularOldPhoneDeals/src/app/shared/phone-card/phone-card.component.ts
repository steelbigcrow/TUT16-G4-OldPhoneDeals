// src/app/components/phone-card/phone-card.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Phone } from '../../models/phone.model';

@Component({
  selector: 'app-phone-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './phone-card.component.html',
  styleUrls: ['./phone-card.component.scss'],
})
export class PhoneCardComponent {
  // input the phone data to display
  @Input() phone!: Phone;

  // output the phone object when clicking "view details"
  @Output() viewDetails = new EventEmitter<Phone>();

  // base URL for backend to load static images
  public serverBaseUrl = 'http://localhost:3000';

  // construct full image URL from relative path
  public getImageUrl(imagePath: string): string {
    if (!imagePath) {
      return '';
    }
    let effectiveImagePath = imagePath;
    if (!imagePath.startsWith('/')) {
      effectiveImagePath = '/' + imagePath;
    }
    return this.serverBaseUrl + effectiveImagePath;
  }

  onView() {
    this.viewDetails.emit(this.phone);
  }
}