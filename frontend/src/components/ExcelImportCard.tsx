import React, { useState, useRef } from 'react';
import { FileSpreadsheet, Upload, Download, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import * as XLSX from 'xlsx';
import { api } from '../services/api';

interface ExcelImportCardProps {
  onImportSuccess?: () => void;
}

export const ExcelImportCard: React.FC<ExcelImportCardProps> = ({ onImportSuccess }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleDownloadTemplate = () => {
    const templateData = [
      ['Title', 'Price', 'Category', 'Image URL', 'Rating'],
      [
        'Apple iPhone 16 Pro Max 256GB Natural Titanium',
        '17800000',
        'Smartfonlar va Gadjetlar',
        'https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=600&q=80',
        '4.9'
      ],
      [
        'MacBook Air 13 M3 16GB 512GB Midnight',
        '14800000',
        'Noutbuklar va Kompyuterlar',
        'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80',
        '4.8'
      ]
    ];

    const worksheet = XLSX.utils.aoa_to_sheet(templateData);

    // Auto-fit column widths
    worksheet['!cols'] = [
      { wch: 45 }, // Title
      { wch: 15 }, // Price
      { wch: 25 }, // Category
      { wch: 60 }, // Image URL
      { wch: 10 }  // Rating
    ];

    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Mahsulotlar');
    XLSX.writeFile(workbook, 'Uzum_Market_Import_Shablon.xlsx');
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setStatusMessage(null);
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (file.name.endsWith('.xlsx') || file.name.endsWith('.xls')) {
        setSelectedFile(file);
      } else {
        setStatusMessage({
          type: 'error',
          text: 'Faqat Excel (.xlsx yoki .xls) fayllari qo\'llab-quvvatlanadi!'
        });
        setSelectedFile(null);
      }
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setStatusMessage(null);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      if (file.name.endsWith('.xlsx') || file.name.endsWith('.xls')) {
        setSelectedFile(file);
      } else {
        setStatusMessage({
          type: 'error',
          text: 'Faqat Excel (.xlsx yoki .xls) fayllari qo\'llab-quvvatlanadi!'
        });
        setSelectedFile(null);
      }
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setIsUploading(true);
    setStatusMessage(null);

    try {
      const result = await api.importProductsFromExcel(selectedFile);
      setStatusMessage({
        type: 'success',
        text: result.message || `Excel faylidan ${result.count || 0} ta mahsulot muvaffaqiyatli import qilindi!`
      });
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      if (onImportSuccess) {
        onImportSuccess();
      }
    } catch (err: any) {
      setStatusMessage({
        type: 'error',
        text: err.message || 'Import qilishda xatolik yuz berdi'
      });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-3xl p-6 shadow-xl border border-gray-100 dark:border-gray-700">
      {/* Header */}
      <div className="flex items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400 rounded-2xl">
            <FileSpreadsheet className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-black text-gray-900 dark:text-white">
              Uzum Market - Excel Mahsulotlarni Boshqarish
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              Uzum Market yoki boshqa do'kon mahsulotlarini Excel fayli orqali bazaga ommaviy yuklang
            </p>
          </div>
        </div>
      </div>

      {/* Drag & Drop Upload Zone */}
      <div
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className="border-2 border-dashed border-emerald-300 dark:border-emerald-700 hover:border-emerald-500 rounded-2xl p-6 text-center cursor-pointer transition-all bg-emerald-50/50 dark:bg-emerald-950/10 hover:bg-emerald-50 dark:hover:bg-emerald-950/20 mb-4"
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".xlsx, .xls"
          onChange={handleFileChange}
          className="hidden"
        />

        <Upload className="w-8 h-8 text-emerald-500 mx-auto mb-2 animate-bounce" />
        <p className="text-xs font-bold text-gray-700 dark:text-gray-300 mb-1">
          {selectedFile ? selectedFile.name : 'Excel faylini tanlash yoki bu yerga tashlash (.xlsx, .xls)'}
        </p>
        <p className="text-[10px] text-gray-400">
          Format: Column 0 (Title), Column 1 (Price), Column 2 (Category), Column 3 (Image URL), Column 4 (Rating)
        </p>
      </div>

      {/* Status Notification */}
      {statusMessage && (
        <div
          className={`flex items-center gap-2 p-3.5 rounded-2xl mb-4 text-xs font-bold ${
            statusMessage.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800'
              : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800'
          }`}
        >
          {statusMessage.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
          )}
          <span>{statusMessage.text}</span>
        </div>
      )}

      {/* Action Buttons: Template Download + Bulk Upload */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <button
          type="button"
          onClick={handleDownloadTemplate}
          className="py-3 px-4 rounded-xl text-xs font-bold bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-800 dark:text-white transition-all flex items-center justify-center gap-2 border border-gray-200 dark:border-gray-600 active:scale-95 cursor-pointer shadow-sm"
        >
          <Download className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
          <span>Excel Shablonini Yuklab Olish</span>
        </button>

        <button
          type="button"
          disabled={!selectedFile || isUploading}
          onClick={handleUpload}
          className={`py-3 px-4 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 shadow-md ${
            selectedFile && !isUploading
              ? 'bg-emerald-600 hover:bg-emerald-700 text-white cursor-pointer active:scale-95'
              : 'bg-gray-200 dark:bg-gray-700 text-gray-400 dark:text-gray-500 cursor-not-allowed'
          }`}
        >
          {isUploading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Import Qilinmoqda...</span>
            </>
          ) : (
            <>
              <Upload className="w-4 h-4" />
              <span>Excel Faylini Bazaga Yuklash</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
