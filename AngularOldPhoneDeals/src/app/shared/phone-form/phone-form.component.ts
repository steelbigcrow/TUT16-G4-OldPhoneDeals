import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {NgClass} from '@angular/common';

// Interface for the data emitted by the form on submit
export interface PhoneFormData {
  title: string;
  brand: string;
  image: string;
  stock: number;
  price: number;
  isDisabled: boolean;
}

// Interface for optional initial data passed to the form (for editing)
export interface PhoneInputData {
  _id?: string; // Optional ID if editing
  title: string;
  brand: string;
  image: string;
  stock: number;
  price: number;
  isDisabled: boolean;
}

@Component({
  selector: 'app-phone-form',
  templateUrl: './phone-form.component.html',
  imports: [
    NgClass,
    ReactiveFormsModule
  ],
  // No specific styleUrls needed as per requirement
})
export class PhoneFormComponent implements OnInit {
  @Input() initialData?: PhoneInputData; // Input for pre-filling the form (edit mode)
  @Input() submitButtonText: string = 'Submit'; // Allow customizing submit button text
  @Input() cancelButtonText: string = 'Cancel'; // Allow customizing cancel button text
  @Input() showCancelButton: boolean = true; // Control visibility of cancel button

  @Output() formSubmit = new EventEmitter<PhoneFormData>(); // Emits form data on valid submission
  @Output() formCancel = new EventEmitter<void>(); // Emits when cancel is clicked

  phoneForm!: FormGroup; // The reactive form group
  brands: string[] = [ // List of allowed brands as per requirements
    'Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry'
  ];

  constructor(private fb: FormBuilder) {} // Inject FormBuilder

  ngOnInit(): void {
    this.initForm(); // Initialize the form structure and validators
    // If initialData is provided, patch the form (for editing)
    if (this.initialData) {
      // Ensure all expected form controls exist before patching
       const formDataToPatch: Partial<PhoneInputData> = {
        title: this.initialData.title,
        brand: this.initialData.brand,
        image: this.initialData.image,
        stock: this.initialData.stock,
        price: this.initialData.price,
        isDisabled: this.initialData.isDisabled ?? false // Default isDisabled if not provided
      };
      this.phoneForm.patchValue(formDataToPatch);
    }
  }

  // Initializes the FormGroup with controls and validators
  private initForm(): void {
    this.phoneForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      brand: ['', Validators.required],
      // Image input accepts URL or specific filenames (basic validation)
      image: ['', [Validators.required, Validators.pattern('(https?://.+|[^\\s/]+\\.(jpe?g|png|gif|bmp)$)')]],
      stock: [0, [Validators.required, Validators.min(0), Validators.pattern('^[0-9]+$')]], // Must be non-negative integer
      price: [0.01, [Validators.required, Validators.min(0.01)]], // Must be positive number
      isDisabled: [false, Validators.required] // Checkbox for disabling the listing
    });
  }

  // --- Form Control Getters for easier template access and validation ---
  get title() { return this.phoneForm.get('title'); }
  get brand() { return this.phoneForm.get('brand'); }
  get image() { return this.phoneForm.get('image'); }
  get stock() { return this.phoneForm.get('stock'); }
  get price() { return this.phoneForm.get('price'); }
  get isDisabled() { return this.phoneForm.get('isDisabled'); }
  // --- End Form Control Getters ---

  // Handles form submission
  onSubmit(): void {
    this.phoneForm.markAllAsTouched(); // Mark all fields as touched to trigger validation messages
    if (this.phoneForm.valid) {
      // Emit the relevant form data, ensuring types match PhoneFormData
       const formData: PhoneFormData = {
         title: this.phoneForm.value.title,
         brand: this.phoneForm.value.brand,
         image: this.phoneForm.value.image,
         stock: Number(this.phoneForm.value.stock), // Ensure stock is number
         price: Number(this.phoneForm.value.price), // Ensure price is number
         isDisabled: Boolean(this.phoneForm.value.isDisabled) // Ensure isDisabled is boolean
       };
      this.formSubmit.emit(formData);
    } else {
      console.error('Phone form is invalid. Please check the fields.');
      // Optionally, scroll to the first invalid field or show a general error message
    }
  }

  // Handles cancel button click
  onCancel(): void {
    this.formCancel.emit(); // Emit the cancel event
  }
}
