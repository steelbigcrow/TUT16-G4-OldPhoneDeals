import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RestApiService } from '../services/rest-api.service';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  standalone: true,
  styleUrls: ['./verify-email.component.scss']
})
export class VerifyEmailComponent implements OnInit {
  message = '';
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private api: RestApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const email = this.route.snapshot.queryParamMap.get('email');
    if (token && email) {
      this.api.verifyEmail(token, email).subscribe({
        next: (res: any) => {
          this.message = res.message || 'Your account has been activated!';
          this.loading = false;
          setTimeout(() => this.router.navigate(['/login']), 3000); // 成功后跳转到登录页面
        },
        error: (err) => {
          this.message = err.error?.message || 'Activation failed.';
          this.loading = false;
        }
      });
    } else {
      this.message = 'Invalid activation link.';
      this.loading = false;
    }
  }

}
