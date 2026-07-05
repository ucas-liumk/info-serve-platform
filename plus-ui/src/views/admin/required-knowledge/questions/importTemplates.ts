export const QUESTION_JSON_TEMPLATE_FILENAME = 'required-knowledge-question-template.json';
export const QUESTION_EXCEL_TEMPLATE_FILENAME = 'required-knowledge-question-template.xlsx';

export const getQuestionTemplateUrl = (filename: string, base = import.meta.env.BASE_URL || '/') => {
  const normalizedBase = base.endsWith('/') ? base : `${base}/`;
  return `${normalizedBase}templates/${filename}`;
};

const downloadQuestionTemplate = (filename: string) => {
  const link = document.createElement('a');
  link.href = getQuestionTemplateUrl(filename);
  link.download = filename;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
};

export const downloadQuestionJsonTemplate = () => {
  downloadQuestionTemplate(QUESTION_JSON_TEMPLATE_FILENAME);
};

export const downloadQuestionExcelTemplate = () => {
  downloadQuestionTemplate(QUESTION_EXCEL_TEMPLATE_FILENAME);
};
