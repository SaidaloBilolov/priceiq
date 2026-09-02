import React, { useState, useRef } from 'react';
import { FileSpreadsheet, Upload, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { api } from '../services/api';

interface ExcelImportCardProps {
  onImportSuccess?: () => void;
}

export const ExcelImportCard: React.FC<ExcelImportCardProps> = ({ onImportSuccess }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

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
      <div className="flex items-center gap-3 mb-4">
        <div className="p-3 bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 rounded-2xl">
          <FileSpreadsheet className="w-6 h-6" />
        </div>
        <div>
          <h3 className="text-base font-bold text-gray-900 dark:text-white">
            Excel Bilan Ommaviy Import (.xlsx)
          </h3>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            Uzum Market yoki boshqa do'kon mahsulotlarini Excel fayli orqali bazaga yuklang
          </p>
        </div>
      </div>

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

        <Upload className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
        <p className="text-xs font-bold text-gray-700 dark:text-gray-300 mb-1">
          {selectedFile ? selectedFile.name : 'Excel faylini tanlash yoki bu yerga tashlash'}
        </p>
        <p className="text-[10px] text-gray-400">
          Format: Column 0 (Nomi), Column 1 (Narxi), Column 2 (Kategoriya), Column 3 (Rasm URL), Column 4 (Reyting)
        </p>
      </div>

      {statusMessage && (
        <div
          className={`flex items-center gap-2 p-3 rounded-xl mb-4 text-xs font-medium ${
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

      <button
        type="button"
        disabled={!selectedFile || isUploading}
        onClick={handleUpload}
        className={`w-full py-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 shadow-md ${
          selectedFile && !isUploading
            ? 'bg-emerald-600 hover:bg-emerald-700 text-white cursor-pointer'
            : 'bg-gray-200 dark:bg-gray-700 text-gray-400 dark:text-gray-500 cursor-not-allowed'
        }`}
      >
        {isUploading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Excel Import Qilinmoqda...</span>
          </>
        ) : (
          <>
            <Upload className="w-4 h-4" />
            <span>Excel Faylini Bazaga Yuklash</span>
          </>
        )}
      </button>
    </div>
  );
};
