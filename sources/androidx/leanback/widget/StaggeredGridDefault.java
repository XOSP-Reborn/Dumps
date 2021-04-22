package androidx.leanback.widget;

import androidx.leanback.widget.StaggeredGrid;

final class StaggeredGridDefault extends StaggeredGrid {
    StaggeredGridDefault() {
    }

    /* access modifiers changed from: package-private */
    public int getRowMax(int i) {
        int i2;
        StaggeredGrid.Location location;
        int i3 = this.mFirstVisibleIndex;
        if (i3 < 0) {
            return Integer.MIN_VALUE;
        }
        if (this.mReversedFlow) {
            int edge = this.mProvider.getEdge(i3);
            if (getLocation(this.mFirstVisibleIndex).row == i) {
                return edge;
            }
            int i4 = this.mFirstVisibleIndex;
            do {
                i4++;
                if (i4 <= getLastIndex()) {
                    location = getLocation(i4);
                    edge += location.offset;
                }
            } while (location.row != i);
            return edge;
        }
        int edge2 = this.mProvider.getEdge(this.mLastVisibleIndex);
        StaggeredGrid.Location location2 = getLocation(this.mLastVisibleIndex);
        if (location2.row != i) {
            int i5 = this.mLastVisibleIndex;
            while (true) {
                i5--;
                if (i5 < getFirstIndex()) {
                    break;
                }
                edge2 -= location2.offset;
                location2 = getLocation(i5);
                if (location2.row == i) {
                    i2 = location2.size;
                    break;
                }
            }
        } else {
            i2 = location2.size;
        }
        return edge2 + i2;
        return Integer.MIN_VALUE;
    }

    /* access modifiers changed from: package-private */
    public int getRowMin(int i) {
        StaggeredGrid.Location location;
        int i2;
        int i3 = this.mFirstVisibleIndex;
        if (i3 < 0) {
            return Integer.MAX_VALUE;
        }
        if (this.mReversedFlow) {
            int edge = this.mProvider.getEdge(this.mLastVisibleIndex);
            StaggeredGrid.Location location2 = getLocation(this.mLastVisibleIndex);
            if (location2.row != i) {
                int i4 = this.mLastVisibleIndex;
                while (true) {
                    i4--;
                    if (i4 < getFirstIndex()) {
                        break;
                    }
                    edge -= location2.offset;
                    location2 = getLocation(i4);
                    if (location2.row == i) {
                        i2 = location2.size;
                        break;
                    }
                }
            } else {
                i2 = location2.size;
            }
            return edge - i2;
        }
        int edge2 = this.mProvider.getEdge(i3);
        if (getLocation(this.mFirstVisibleIndex).row == i) {
            return edge2;
        }
        int i5 = this.mFirstVisibleIndex;
        do {
            i5++;
            if (i5 <= getLastIndex()) {
                location = getLocation(i5);
                edge2 += location.offset;
            }
        } while (location.row != i);
        return edge2;
        return Integer.MAX_VALUE;
    }

    @Override // androidx.leanback.widget.Grid
    public int findRowMax(boolean z, int i, int[] iArr) {
        int i2;
        int edge = this.mProvider.getEdge(i);
        StaggeredGrid.Location location = getLocation(i);
        int i3 = location.row;
        if (this.mReversedFlow) {
            int i4 = i + 1;
            i2 = i;
            int i5 = edge;
            int i6 = i3;
            int i7 = 1;
            while (i7 < this.mNumRows && i4 <= this.mLastVisibleIndex) {
                StaggeredGrid.Location location2 = getLocation(i4);
                i5 += location2.offset;
                int i8 = location2.row;
                if (i8 != i6) {
                    i7++;
                    if (!z ? i5 >= edge : i5 <= edge) {
                        i6 = i8;
                    } else {
                        i2 = i4;
                        edge = i5;
                        i3 = i8;
                        i6 = i3;
                    }
                }
                i4++;
            }
        } else {
            int i9 = i - 1;
            StaggeredGrid.Location location3 = location;
            int i10 = i3;
            int i11 = edge;
            edge = this.mProvider.getSize(i) + edge;
            i2 = i;
            int i12 = 1;
            while (i12 < this.mNumRows && i9 >= this.mFirstVisibleIndex) {
                i11 -= location3.offset;
                location3 = getLocation(i9);
                int i13 = location3.row;
                if (i13 != i10) {
                    i12++;
                    int size = this.mProvider.getSize(i9) + i11;
                    if (!z ? size >= edge : size <= edge) {
                        i10 = i13;
                    } else {
                        i2 = i9;
                        edge = size;
                        i3 = i13;
                        i10 = i3;
                    }
                }
                i9--;
            }
        }
        if (iArr != null) {
            iArr[0] = i3;
            iArr[1] = i2;
        }
        return edge;
    }

    @Override // androidx.leanback.widget.Grid
    public int findRowMin(boolean z, int i, int[] iArr) {
        int i2;
        int i3;
        int edge = this.mProvider.getEdge(i);
        StaggeredGrid.Location location = getLocation(i);
        int i4 = location.row;
        if (this.mReversedFlow) {
            i2 = edge - this.mProvider.getSize(i);
            int i5 = i - 1;
            StaggeredGrid.Location location2 = location;
            int i6 = i4;
            int i7 = edge;
            i3 = i;
            int i8 = 1;
            while (i8 < this.mNumRows && i5 >= this.mFirstVisibleIndex) {
                i7 -= location2.offset;
                location2 = getLocation(i5);
                int i9 = location2.row;
                if (i9 != i6) {
                    i8++;
                    int size = i7 - this.mProvider.getSize(i5);
                    if (!z ? size >= i2 : size <= i2) {
                        i6 = i9;
                    } else {
                        i3 = i5;
                        i2 = size;
                        i4 = i9;
                        i6 = i4;
                    }
                }
                i5--;
            }
        } else {
            int i10 = i + 1;
            i2 = edge;
            int i11 = i2;
            int i12 = i4;
            i3 = i;
            int i13 = 1;
            while (i13 < this.mNumRows && i10 <= this.mLastVisibleIndex) {
                StaggeredGrid.Location location3 = getLocation(i10);
                i11 += location3.offset;
                int i14 = location3.row;
                if (i14 != i12) {
                    i13++;
                    if (!z ? i11 >= i2 : i11 <= i2) {
                        i12 = i14;
                    } else {
                        i3 = i10;
                        i2 = i11;
                        i4 = i14;
                        i12 = i4;
                    }
                }
                i10++;
            }
        }
        if (iArr != null) {
            iArr[0] = i4;
            iArr[1] = i3;
        }
        return i2;
    }

    private int findRowEdgeLimitSearchIndex(boolean z) {
        boolean z2 = false;
        if (z) {
            for (int i = this.mLastVisibleIndex; i >= this.mFirstVisibleIndex; i--) {
                int i2 = getLocation(i).row;
                if (i2 == 0) {
                    z2 = true;
                } else if (z2 && i2 == this.mNumRows - 1) {
                    return i;
                }
            }
            return -1;
        }
        for (int i3 = this.mFirstVisibleIndex; i3 <= this.mLastVisibleIndex; i3++) {
            int i4 = getLocation(i3).row;
            if (i4 == this.mNumRows - 1) {
                z2 = true;
            } else if (z2 && i4 == 0) {
                return i3;
            }
        }
        return -1;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0107 A[LOOP:2: B:81:0x0107->B:95:0x012b, LOOP_START, PHI: r6 r7 r10 
      PHI: (r6v9 int) = (r6v3 int), (r6v12 int) binds: [B:80:0x0105, B:95:0x012b] A[DONT_GENERATE, DONT_INLINE]
      PHI: (r7v8 int) = (r7v6 int), (r7v9 int) binds: [B:80:0x0105, B:95:0x012b] A[DONT_GENERATE, DONT_INLINE]
      PHI: (r10v4 int) = (r10v2 int), (r10v6 int) binds: [B:80:0x0105, B:95:0x012b] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0139  */
    @Override // androidx.leanback.widget.StaggeredGrid
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean appendVisibleItemsWithoutCache(int r14, boolean r15) {
        /*
        // Method dump skipped, instructions count: 355
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.leanback.widget.StaggeredGridDefault.appendVisibleItemsWithoutCache(int, boolean):boolean");
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0101 A[LOOP:2: B:80:0x0101->B:94:0x0125, LOOP_START, PHI: r5 r6 r9 
      PHI: (r5v9 int) = (r5v3 int), (r5v12 int) binds: [B:79:0x00ff, B:94:0x0125] A[DONT_GENERATE, DONT_INLINE]
      PHI: (r6v8 int) = (r6v6 int), (r6v9 int) binds: [B:79:0x00ff, B:94:0x0125] A[DONT_GENERATE, DONT_INLINE]
      PHI: (r9v3 int) = (r9v1 int), (r9v5 int) binds: [B:79:0x00ff, B:94:0x0125] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x0133  */
    @Override // androidx.leanback.widget.StaggeredGrid
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean prependVisibleItemsWithoutCache(int r13, boolean r14) {
        /*
        // Method dump skipped, instructions count: 351
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.leanback.widget.StaggeredGridDefault.prependVisibleItemsWithoutCache(int, boolean):boolean");
    }
}
