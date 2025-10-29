import { Component, OnInit, DoCheck } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';
import { DataService } from '../../services/data.service';
import { PaginationComponent } from '../../shared/pagination/pagination.component';

interface Transaction {
  id: string;
  timestamp: Date;
  buyer: string;
  items: {
    name: string;
    quantity: number;
    price: number;
  }[];
  totalAmount: number;
  status: 'completed' | 'pending' | 'cancelled';
}

@Component({
  selector: 'app-admin-sales-logs',
  templateUrl: './admin-sales-logs.component.html',
  styleUrls: ['./admin-sales-logs.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent]
})
export class AdminSalesLogsComponent implements OnInit, DoCheck {
  transactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];
  private prevMessage: string = '';
  searchForm: FormGroup;
  filterForm: FormGroup;
  sortOptions = [
    { label: 'Default', value: 'default' },
    { label: 'Timestamp (Newest to Oldest)', value: 'timestampDesc' },
    { label: 'Timestamp (Oldest to Newest)', value: 'timestampAsc' },
    { label: 'Total Amount (High to Low)', value: 'amountDesc' },
    { label: 'Total Amount (Low to High)', value: 'amountAsc' }
  ];
  brands = ['All Brands', 'Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry'];
  totalSales = 0;
  totalTransactions = 0;
  loading = false;

  // Pagination properties
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  totalItems = 0;

  // Global sales statistics
  globalTotalSales = 0;
  globalTotalTransactions = 0;

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private data: DataService
  ) {
    this.searchForm = this.fb.group({
      searchTerm: ['']
    });

    this.filterForm = this.fb.group({
      sortOption: ['default'],
      brandFilter: ['All Brands']
    });
  }

  ngOnInit(): void {
    this.loadTransactions();
    this.loadSalesStats();
    this.setupSearch();
    this.setupFilter();
  }

  ngDoCheck(): void {
    const msg = this.data.message;
    if (msg && msg !== this.prevMessage) {
      this.prevMessage = msg;
      if (msg === 'Your order has been successfully delivered！') {
        this.loadTransactions();
      }
    }
  }

  async loadTransactions(): Promise<void> {
    this.loading = true;
    try {
      const searchTerm = this.searchForm.get('searchTerm')?.value;
      const brandFilter = this.filterForm.get('brandFilter')?.value;
      const sortOption = this.filterForm.get('sortOption')?.value;
      const response = await this.adminService.getSalesLogs(
        this.currentPage,
        this.itemsPerPage,
        searchTerm,
        brandFilter,
        sortOption
      );

      if (response.success) {
        // Map backend orders to frontend transactions
        const rawOrders = response.orders;
        this.transactions = rawOrders.map((order: any) => ({
          id: order._id,
          timestamp: new Date(order.createdAt),
          buyer: `${order.userId.firstName} ${order.userId.lastName}`,
          items: order.items.map((item: any) => ({
            name: item.title,
            quantity: item.quantity,
            price: item.price
          })),
          totalAmount: order.totalAmount
        }));
        this.totalItems = response.total;
        this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
        this.filteredTransactions = this.transactions;
      } else {
        this.data.error(response.message || 'Failed to load transactions');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading transactions');
    } finally {
      this.loading = false;
    }
  }

  setupSearch(): void {
    this.searchForm.get('searchTerm')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.currentPage = 1;
        this.loadTransactions();
      });
  }

  setupFilter(): void {
    this.filterForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.currentPage = 1;
        this.loadTransactions();
      });
  }

  exportToCSV(): void {
    const searchTerm = this.searchForm.get('searchTerm')?.value;
    const brandFilter = this.filterForm.get('brandFilter')?.value;
    const sortOption = this.filterForm.get('sortOption')?.value;
    this.adminService.exportSalesLogs('csv', searchTerm, brandFilter, sortOption)
      .then((blob: Blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `sales_log_${new Date().toISOString()}.csv`;
        link.click();
      })
      .catch(error => this.data.error(error.message || 'Export CSV failed'));
  }

  exportToJSON(): void {
    const searchTerm = this.searchForm.get('searchTerm')?.value;
    const brandFilter = this.filterForm.get('brandFilter')?.value;
    const sortOption = this.filterForm.get('sortOption')?.value;
    this.adminService.exportSalesLogs('json', searchTerm, brandFilter, sortOption)
      .then((blob: Blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `sales_log_${new Date().toISOString()}.json`;
        link.click();
      })
      .catch(error => this.data.error(error.message || 'Export JSON failed'));
  }

  // Load global sales statistics
  async loadSalesStats(): Promise<void> {
    try {
      const response = await this.adminService.getSalesStats();
      if (response.success) {
        this.globalTotalSales = response.totalSales;
        this.globalTotalTransactions = response.totalTransactions;
      } else {
        this.data.error(response.message || 'Failed to load sales stats');
      }
    } catch (error: any) {
      this.data.error(error.message || 'An error occurred while loading sales stats');
    }
  }

  // Pagination controls
  goToPrevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadTransactions();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadTransactions();
    }
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadTransactions();
  }
}
