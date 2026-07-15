import { describe, expect, it } from 'vitest';
import type { CategoryTreeNode } from '@/api/infoservice/types';
import {
  buildSelectedChips,
  decodeCategoryCodes,
  encodeCategoryCodes,
  getGroupCheckState,
  groupResourceCount,
  removeCategory,
  resolveSelectionTitle,
  toggleCategory,
  toggleGroup
} from './categoryFacets';

const tree: CategoryTreeNode[] = [
  {
    categoryId: 300000,
    categoryCode: 'general',
    categoryName: '综合资料',
    orderNum: 0,
    children: [
      { categoryId: 300001, categoryCode: 'policy', categoryName: '政策制度', orderNum: 1, resourceCount: 6 },
      { categoryId: 300002, categoryCode: 'tech', categoryName: '技术文档', orderNum: 2, resourceCount: 0 },
      { categoryId: 300003, categoryCode: 'template', categoryName: '常用模板', orderNum: 3, resourceCount: 2 }
    ]
  },
  { categoryId: 300100, categoryCode: 'blank', categoryName: '空栏目', orderNum: 1, children: [] }
];

const generalGroup = tree[0];
const blankGroup = tree[1];

describe('encodeCategoryCodes / decodeCategoryCodes', () => {
  it('encodes selected codes into a comma string', () => {
    expect(encodeCategoryCodes(['policy', 'tech'])).toBe('policy,tech');
    expect(encodeCategoryCodes(['policy'])).toBe('policy');
  });

  it('returns undefined for an empty selection (all)', () => {
    expect(encodeCategoryCodes([])).toBeUndefined();
  });

  it('drops blanks, the all sentinel and duplicates when encoding', () => {
    expect(encodeCategoryCodes([' policy ', '', 'all', 'policy', 'tech'])).toBe('policy,tech');
    expect(encodeCategoryCodes(['all', '  '])).toBeUndefined();
  });

  it('decodes a comma string into codes', () => {
    expect(decodeCategoryCodes('policy,tech')).toEqual(['policy', 'tech']);
    expect(decodeCategoryCodes(' policy , tech ,,policy')).toEqual(['policy', 'tech']);
  });

  it('decodes empty, nullish and all sentinel into empty selection', () => {
    expect(decodeCategoryCodes('')).toEqual([]);
    expect(decodeCategoryCodes(undefined)).toEqual([]);
    expect(decodeCategoryCodes(null)).toEqual([]);
    expect(decodeCategoryCodes('all')).toEqual([]);
  });
});

describe('groupResourceCount', () => {
  it('sums children resource counts', () => {
    expect(groupResourceCount(generalGroup)).toBe(8);
  });

  it('returns 0 for a group without children', () => {
    expect(groupResourceCount(blankGroup)).toBe(0);
    expect(groupResourceCount({ ...blankGroup, children: undefined })).toBe(0);
  });

  it('treats children missing resourceCount as 0', () => {
    const group: CategoryTreeNode = {
      ...generalGroup,
      children: [{ categoryId: 1, categoryCode: 'x', categoryName: 'X' }]
    };
    expect(groupResourceCount(group)).toBe(0);
  });
});

describe('getGroupCheckState', () => {
  it('is none when no child is selected', () => {
    expect(getGroupCheckState(generalGroup, [])).toBe('none');
    expect(getGroupCheckState(generalGroup, ['other'])).toBe('none');
  });

  it('is partial when some but not all children are selected', () => {
    expect(getGroupCheckState(generalGroup, ['policy'])).toBe('partial');
    expect(getGroupCheckState(generalGroup, ['policy', 'tech'])).toBe('partial');
  });

  it('is all when every child is selected', () => {
    expect(getGroupCheckState(generalGroup, ['policy', 'tech', 'template'])).toBe('all');
    expect(getGroupCheckState(generalGroup, ['policy', 'tech', 'template', 'other'])).toBe('all');
  });

  it('is none for a group without children', () => {
    expect(getGroupCheckState(blankGroup, ['policy'])).toBe('none');
  });
});

describe('toggleCategory', () => {
  it('adds an unselected code without mutating the input', () => {
    const before = ['policy'];
    const after = toggleCategory(before, 'tech');
    expect(after).toEqual(['policy', 'tech']);
    expect(before).toEqual(['policy']);
    expect(after).not.toBe(before);
  });

  it('removes an already selected code without mutating the input', () => {
    const before = ['policy', 'tech'];
    const after = toggleCategory(before, 'policy');
    expect(after).toEqual(['tech']);
    expect(before).toEqual(['policy', 'tech']);
  });
});

describe('toggleGroup', () => {
  it('selects every child when the group is not fully selected', () => {
    expect(toggleGroup([], generalGroup)).toEqual(['policy', 'tech', 'template']);
    expect(toggleGroup(['policy'], generalGroup)).toEqual(['policy', 'tech', 'template']);
  });

  it('keeps codes from other groups when selecting', () => {
    expect(toggleGroup(['other'], generalGroup)).toEqual(['other', 'policy', 'tech', 'template']);
  });

  it('unselects every child when the group is fully selected', () => {
    expect(toggleGroup(['policy', 'tech', 'template', 'other'], generalGroup)).toEqual(['other']);
  });

  it('returns the selection unchanged for a group without children', () => {
    const before = ['policy'];
    const after = toggleGroup(before, blankGroup);
    expect(after).toEqual(['policy']);
    expect(before).toEqual(['policy']);
  });

  it('does not mutate the input selection', () => {
    const before = ['policy'];
    toggleGroup(before, generalGroup);
    expect(before).toEqual(['policy']);
  });
});

describe('removeCategory', () => {
  it('removes a chip code without mutating the input', () => {
    const before = ['policy', 'tech'];
    const after = removeCategory(before, 'tech');
    expect(after).toEqual(['policy']);
    expect(before).toEqual(['policy', 'tech']);
  });

  it('returns an equal selection when the code is absent', () => {
    expect(removeCategory(['policy'], 'missing')).toEqual(['policy']);
  });
});

describe('buildSelectedChips', () => {
  it('maps selected codes to chips with names in selection order', () => {
    expect(buildSelectedChips(tree, ['tech', 'policy'])).toEqual([
      { code: 'tech', name: '技术文档' },
      { code: 'policy', name: '政策制度' }
    ]);
  });

  it('falls back to the code for stale selections so they stay removable', () => {
    expect(buildSelectedChips(tree, ['gone'])).toEqual([{ code: 'gone', name: 'gone' }]);
  });

  it('returns no chips for an empty selection', () => {
    expect(buildSelectedChips(tree, [])).toEqual([]);
  });
});

describe('resolveSelectionTitle', () => {
  it('is 全部资料 when nothing is selected', () => {
    expect(resolveSelectionTitle(tree, [])).toBe('全部资料');
  });

  it('is the category name for a single selection', () => {
    expect(resolveSelectionTitle(tree, ['policy'])).toBe('政策制度');
  });

  it('falls back for a single stale selection', () => {
    expect(resolveSelectionTitle(tree, ['gone'])).toBe('当前分类');
  });

  it('counts categories for a multi selection', () => {
    expect(resolveSelectionTitle(tree, ['policy', 'tech'])).toBe('已选 2 个分类');
  });
});
