import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-quantity-input-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <mat-form-field appearance="fill">
        <mat-label>Quantity</mat-label>
        <input matInput type="number" [formControl]="quantityControl" min="1">
        <mat-error *ngIf="quantityControl.hasError('required')">Quantity is required</mat-error>
        <mat-error *ngIf="quantityControl.hasError('min')">Quantity must be at least 1</mat-error>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" 
              [disabled]="!quantityControl.valid"
              (click)="onConfirm()">
        Confirm
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-form-field {
      width: 100%;
    }
  `]
})
export class QuantityInputDialogComponent {
  quantityControl = new FormControl(1, [
    Validators.required,
    Validators.min(1)
  ]);

  constructor(
    public dialogRef: MatDialogRef<QuantityInputDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { title: string }
  ) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.quantityControl.valid) {
      this.dialogRef.close(this.quantityControl.value);
    }
  }
} 