import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataService } from '../services/data.service';

@Component({
  selector: 'app-message',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './message.component.html',
  styleUrls: ['./message.component.scss']
})
export class MessageComponent {
  private timer: any;
  constructor(public data: DataService) {}

  ngDoCheck() {
    if (this.data.message && !this.timer) {
      this.timer = setTimeout(() => {
        this.close();
      }, 3000); // 3 秒后自动关闭
    }
  }
  close() {
    this.data.message = '';
    clearTimeout(this.timer);
    this.timer = null;
  }
}
