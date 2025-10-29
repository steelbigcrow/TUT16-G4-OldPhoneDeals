import {Component, AfterViewInit, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {RouterModule, Router, NavigationEnd} from '@angular/router';
import { DataService } from '../services/data.service';
import {Subscription} from 'rxjs';

declare var bootstrap: any; // 声明bootstrap变量，让TypeScript知道它的存在

@Component({
  selector: 'app-user-header',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,   // 支持 [(ngModel)] 双向绑定
    RouterModule   // 支持路由功能
  ],
  templateUrl: './user-header.component.html', // 分离到外部 HTML
  styleUrls: ['./user-header.component.scss']  // 样式文件路径
})
export class UserHeaderComponent implements AfterViewInit, OnInit {
  siteName = 'OldPhoneDeals'

  // 搜索相关
  searchQuery = '';
  selectedBrand = '';
  maxPrice = 1000;

  // 品牌选项
  brands = [
    'Apple',
    'Samsung',
    'BlackBerry',
    'Sony',
    'Huawei',
    'HTC',
    'Nokia',
    'LG',
    'Motorola'
  ];

  // 用户状态
  cartItemCount = 0;
  user : any
  private routerSubscription: Subscription | null = null;

  ngOnInit(): void {
    // 取得登入后的本地用户信息

    // this.data.refreshUser();  // 刷新用户状态，调用后端验证
    // console.log("前端看看 user name",this.data.currentUser$)
    // this.user = JSON.parse(localStorage.getItem('user') || '{}');


    // 订阅 DataService 的 currentUser$，实时更新 user 变量
    this.data.currentUser$.subscribe(user => {
      this.user = user;
      console.log('用户信息变更:', user);
    });

    // 调用刷新用户方法，触发后端验证
    // 订阅路由事件，每次路由结束时刷新用户状态
    this.routerSubscription = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.data.refreshUser();  // 调用后端验证更新
      }
    });

    // console.log("前端 header打印是否登入", this.isLoggedIn)
    // 从 DataService 获取用户信息
    // this.data.currentUser$.subscribe(user => {
    //   this.user = user;
    // });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }
  /**
   * Expose currentUser$ from DataService via a getter to avoid using this.data in initializer
   */
  get currentUser$() {
    return this.data.currentUser$;
  }

  constructor(
    private router: Router,
    public data: DataService
  ) {}

  ngAfterViewInit() {
    // 初始化Bootstrap组件
    this.initializeBootstrapComponents();
  }

  // 初始化Bootstrap组件
  private initializeBootstrapComponents() {
    // 初始化下拉菜单
    const dropdownElementList = document.querySelectorAll('.dropdown-toggle');
    if (dropdownElementList.length > 0 && typeof bootstrap !== 'undefined') {
      dropdownElementList.forEach(dropdownToggle => {
        new bootstrap.Dropdown(dropdownToggle);
      });
    }
  }

  // 搜索功能
  performSearch() {
    // 构建查询参数
    const queryParams: any = {};

    if (this.searchQuery.trim()) {
      queryParams.q = this.searchQuery.trim();
    }

    if (this.selectedBrand) {
      queryParams.brand = this.selectedBrand;
    }

    // 修改：无论 maxPrice 是否等于默认值，都始终将其加入 URL 参数
    if (this.maxPrice != null) {
      queryParams.maxPrice = this.maxPrice;
    }

    // 导航到搜索页面并传递参数
    this.router.navigate(['/search'], { queryParams });

    // 在移动设备上，搜索后自动关闭导航栏
    const navbarCollapse = document.getElementById('navbarContent');
    if (navbarCollapse && navbarCollapse.classList.contains('show') && typeof bootstrap !== 'undefined') {
      const bsCollapse = bootstrap.Collapse.getInstance(navbarCollapse);
      if (bsCollapse) {
        bsCollapse.hide();
      }
    }
  }

  // 用户操作
  login() {
    this.router.navigate(['/login']);
  }

  logout() {
    if (window.confirm('ready to logout?')) {
      this.data.logoutUser();
      this.router.navigate(['/user-home']);
    }
  }

  // 导航到结算页面
  navigateToCheckout() {
    if (this.isLoggedIn) {
      this.router.navigate(['/checkout']);
    } else {
      this.router.navigate(
        ['/login'],
        { queryParams: { returnUrl: '/checkout' } }
      );
    }
  }

  // 新增：导航到个人资料
  navigateToProfile() {
    this.router.navigate(['/user-profile']);
  }

  // 新增：导航到收藏列表
  navigateToWishlist() {
    if (this.isLoggedIn) {
      this.router.navigate(['/wishlist']);
    } else {
      this.router.navigate(
        ['/login'],
        { queryParams: { returnUrl: '/wishlist' } }
      );
    }
  }

  get isLoggedIn(): boolean {
    return localStorage.getItem('token') !== null;
  }
}
