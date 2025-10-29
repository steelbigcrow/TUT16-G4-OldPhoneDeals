import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Phone } from '../models/phone.model';
import { PhoneCardComponent } from '../shared/phone-card/phone-card.component';
import {RestApiService} from '../services/rest-api.service';
import {DataService} from '../services/data.service';

@Component({
  selector: 'app-user-home',
  templateUrl: './user-home.component.html',
  styleUrls: ['./user-home.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, PhoneCardComponent]
})
export class UserHomeComponent implements OnInit {
  // Phone listings
  soldOutSoonPhones: Phone[] = [];
  bestSellerPhones: Phone[] = [];

  // Loading states
  loadingSoldOutSoon = false;
  loadingBestSellers = false;

  public serverBaseUrl = 'http://localhost:3000'; // Define the base URL for your backend server

  constructor(
    private http: HttpClient,
    private router: Router,
    private rest: RestApiService,
    private data: DataService
  ) {}

  ngOnInit(): void {
    // Load real data from the backend
    this.loadSoldOutSoonPhones();
    this.loadBestSellerPhones();

    // Initialize with mock data - removed
  }

  // Load phones with least stock (more than 0)
  loadSoldOutSoonPhones(): void {
    this.loadingSoldOutSoon = true;
    this.http.get<Phone[]>('/api/phones?special=soldOutSoon').subscribe({
      next: (phones) => {
        this.soldOutSoonPhones = phones;
        this.loadingSoldOutSoon = false;
      },
      error: (error) => {
        console.error('Error loading sold out soon phones:', error);
        this.loadingSoldOutSoon = false;
      }
    });
  }

  // Load phones with highest average rating
  loadBestSellerPhones(): void {
    this.loadingBestSellers = true;
    this.http.get<Phone[]>('/api/phones?special=bestSellers').subscribe({
      next: (phones) => {
        this.bestSellerPhones = phones;
        this.loadingBestSellers = false;
      },
      error: (error) => {
        console.error('Error loading best seller phones:', error);
        this.loadingBestSellers = false;
      }
    });
  }

  // View phone details - redirects to phone detail component
  async viewPhoneDetails(phoneOrId: string | any): Promise<void> {
    let id: string;
    if (typeof phoneOrId === 'string') {
      id = phoneOrId;
    } else {
      // Handle both front-end Phone and back-end _id
      id = phoneOrId.id ?? phoneOrId._id;
    }
    try {
      console.log("前端看看 phone id", id)
      const res: any = await this.rest.get(`http://localhost:3000/api/phones/${id}`);  // 你的 RestApiService 的 GET 方法
      console.log("前端看看 phone res", res)
      if (res.success === false) {
        // 手机不存在或被禁用
        this.data.error('The phone has been deleted or disabled, and details cannot be viewed');
        this.loadSoldOutSoonPhones();
        this.loadBestSellerPhones();
      } else {
        // 存在则跳转
        this.router.navigate(['/phone', id]);
      }
    } catch (error) {

      this.data.error("Get a phone error:" + error);
    }
  }

  // Helper method to construct the full image URL
  getImageUrl(imagePath: string): string {
    if (!imagePath) {
      return ''; // Return empty string or a path to a default placeholder image
    }
    // Ensure there's exactly one slash between the base URL and the image path
    let effectiveImagePath = imagePath;
    if (!imagePath.startsWith('/')) {
      effectiveImagePath = '/' + imagePath;
    }
    return this.serverBaseUrl + effectiveImagePath;
  }
}
