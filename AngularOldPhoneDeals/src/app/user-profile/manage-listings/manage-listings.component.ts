// 删除 import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
// 替换后的组件代码如下：

import { Component, OnInit } from '@angular/core';
import { Phone } from '../../models/phone.model';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { RestApiService } from '../../services/rest-api.service';
import {FormsModule} from '@angular/forms';
import {DataService} from '../../services/data.service';
import { PaginationComponent } from '../../shared/pagination/pagination.component';

@Component({
  selector: 'app-manage-listings',
  templateUrl: './manage-listings.component.html',
  standalone: true,
  styleUrls: ['./manage-listings.component.scss'],
  imports: [CommonModule, RouterModule, FormsModule, PaginationComponent]
})
export class ManageListingsComponent implements OnInit {
  phones: Phone[] = [];
  // Pagination parameters
  totalPages = 1;
  currentPage = 1;
  pageSize = 10;
  allPhones: Phone[] = [];
  user: any;
  userId = '';
  selectedPhoneToDelete: Phone | null = null;
  showDeleteModal = false;
  showToggleModal = false;
  selectedPhoneToToggle: Phone | null = null;
  originalToggleValue: boolean = false;
  // upload image
  selectedFile: File | null = null;
  // add phone
  showAddPhoneModal = false;
  private serverBaseUrl = 'http://localhost:3000';
  newPhone: Partial<Phone> = {
    title: '',
    brand: '',
    image: '',
    stock: 0,
    price: 0
  };
// 允许的品牌列表
  allowedBrands = ['Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry'];
// 添加加载指示器和"成功提示"
  loading = false;
  successMessage = '';



  constructor(
    private http: HttpClient,
    private router: Router,
    private rest: RestApiService,
    private data: DataService
  ) {}

  ngOnInit(): void {
    this.user = JSON.parse(localStorage.getItem('user') || '{}');
    this.userId = this.user.id;
    this.loadPhonesByUserId();
  }

  async loadPhonesByUserId(): Promise<void> {
    try {
      const phones = await this.rest.get(`http://localhost:3000/api/phones/seller/${this.userId}`);
      this.allPhones = [...phones];
      this.applyPagination();
    } catch (error) {
      console.error('Error loading phones by seller:', error);
    }
  }

  getImageUrl(imagePath: string): string {
    if (!imagePath) return '';
    return this.serverBaseUrl + (imagePath.startsWith('/') ? imagePath : '/' + imagePath);
  }

  // 文件选择处理
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      // 不在这里上传，延后到 createPhone 再统一处理
    }
  }



  confirmDelete(phone: Phone): void {
    this.selectedPhoneToDelete = phone;
    this.showDeleteModal = true;
  }

  async deletePhone(): Promise<void> {
    if (this.selectedPhoneToDelete) {
      try {
        console.log("前端 deletePhone", this.selectedPhoneToDelete)
        await this.rest.delete(`http://localhost:3000/api/phones/delete-phone/${this.selectedPhoneToDelete.id}`);
        this.allPhones = this.allPhones.filter(p => p !== this.selectedPhoneToDelete);
        this.applyPagination();
      } catch (err) {
        console.error('Delete failed', err);
      } finally {
        this.showDeleteModal = false;
        this.selectedPhoneToDelete = null;
      }
    }
  }

  onToggleClick(event: MouseEvent, phone: Phone) {
    event.preventDefault();  // 阻止checkbox自动切换
    this.selectedPhoneToToggle = phone;
    this.originalToggleValue = phone.isDisabled;
    this.showToggleModal = true;
  }

// 修改 togglePhone 逻辑
  async togglePhone(): Promise<void> {
    if (this.selectedPhoneToToggle) {
      try {
        // 这里真正切换值
        this.selectedPhoneToToggle.isDisabled = !this.selectedPhoneToToggle.isDisabled;
        const response = await this.rest.put(
          `http://localhost:3000/api/phones/disable-phone/${this.selectedPhoneToToggle.id}`,
          { isDisabled: this.selectedPhoneToToggle.isDisabled });
        if (response.success) {
          this.data.success(response.message );
        } else {
          this.data.error(response.message );
        }


      } catch (err) {
        console.error('Toggle failed', err);
        // 如果失败，回退状态
        this.selectedPhoneToToggle.isDisabled = this.originalToggleValue;
      } finally {
        this.showToggleModal = false;
        this.selectedPhoneToToggle = null;
      }
    }
  }
// 取消操作时，直接关闭模态框即可，状态没变
  cancelToggle(): void {
    this.showToggleModal = false;
    this.selectedPhoneToToggle = null;
  }

  async createPhone(): Promise<void> {
    this.loading = true;
    try {
      // 验证品牌是否合法
      if (!this.allowedBrands.includes(<string>this.newPhone.brand,)) {
        return this.data.error('Invalid brand')
      }
      // 验证库存和价格不能为负数
      if ((this.newPhone.stock ?? 0) < 0 || (this.newPhone.price ?? 0) < 0) {
        return this.data.error('Stock and price must be non-negative');
      }
      // 第一步：上传图片（如果选中了文件）
      if (this.selectedFile) {
        const formData = new FormData();
        formData.append('image', this.selectedFile);
        const uploadResponse = await this.rest.post('http://localhost:3000/api/phones/upload-image', formData);
        this.newPhone.image = uploadResponse?.url; // 服务器返回的图片URL

      }


      const data = await this.rest.post(`http://localhost:3000/api/phones/add-phone`,
        {
          title : this.newPhone.title,
          brand : this.newPhone.brand,
          image : this.newPhone.image,
          stock : this.newPhone.stock,
          seller : this.userId,
          price : this.newPhone.price,

      });
      console.log("前端 data.success", data.success)
      if (data.success) {
        // alert('Add new phone list successfully')

        // // 添加成功后重新加载手机列表
        // await this.loadPhonesByUserId();

        // this.phones.push(created.phone); // 更新前端列表
        // this.showAddPhoneModal = false;

        // 清空表单
        this.newPhone = {
          title: '',
          brand: '',
          image: '',
          stock: 0,
          price: 0
        };

        this.selectedFile = null;
        this.showAddPhoneModal = false;

        this.data.success(data.message)

        await this.loadPhonesByUserId(); // 刷新列表

      } else {
        this.data.error(data.message || 'Add phone failed');
      }

    } catch (error: any) {
      // 检查错误对象并提取错误消息
      const errorMessage = error?.error?.message || 'An unknown error occurred during add new phone';
      // 使用提取的错误消息
      this.data.error(errorMessage);
    }
  }

  // Apply client-side pagination
  applyPagination(): void {
    this.totalPages = Math.ceil(this.allPhones.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.pageSize;
    this.phones = this.allPhones.slice(startIndex, startIndex + this.pageSize);
  }

  // Handle page change from pagination component
  onPageChange(page: number): void {
    this.currentPage = page;
    this.applyPagination();
  }

}
