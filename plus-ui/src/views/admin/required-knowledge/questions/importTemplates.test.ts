import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import readXlsxFile from 'read-excel-file/node';
import { getQuestionTemplateUrl, QUESTION_EXCEL_TEMPLATE_FILENAME, QUESTION_JSON_TEMPLATE_FILENAME } from './importTemplates';

const headers = ['科目', '题型', '题干', '选项', '答案', '解析', '状态'];
const templatePath = (filename: string) => new URL(`../../../../../public/templates/${filename}`, import.meta.url);

describe('required knowledge import templates', () => {
  it('uses stable static download urls with file extensions', () => {
    expect(getQuestionTemplateUrl(QUESTION_JSON_TEMPLATE_FILENAME, '/')).toBe('/templates/required-knowledge-question-template.json');
    expect(getQuestionTemplateUrl(QUESTION_EXCEL_TEMPLATE_FILENAME, '/')).toBe('/templates/required-knowledge-question-template.xlsx');
  });

  it('provides a valid json template', async () => {
    const content = await readFile(templatePath(QUESTION_JSON_TEMPLATE_FILENAME), 'utf-8');
    const payload = JSON.parse(content);

    expect(payload.questions).toHaveLength(2);
    expect(Object.keys(payload.questions[0])).toEqual(headers);
    expect(payload.questions[0]).toMatchObject({
      科目: '软考高项',
      题型: '单选',
      答案: 'B',
      状态: '已发布'
    });
  });

  it('provides a valid xlsx template', async () => {
    const rows = await readXlsxFile(fileURLToPath(templatePath(QUESTION_EXCEL_TEMPLATE_FILENAME)));

    expect(rows).toHaveLength(3);
    expect(rows[0]).toEqual(headers);
    expect(rows[1][0]).toBe('软考高项');
    expect(rows[1][1]).toBe('单选');
    expect(rows[1][4]).toBe('B');
  });
});
