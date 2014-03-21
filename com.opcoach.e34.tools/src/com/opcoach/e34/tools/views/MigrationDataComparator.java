package com.opcoach.e34.tools.views;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

/**
	 * An entry comparator for the table, dealing with column index, keys and
	 * values
	 */
	public class MigrationDataComparator extends ViewerComparator
	{
		private int columnIndex;
		private int direction;
		private ILabelProvider labelProvider;

		public MigrationDataComparator(int columnIndex, ILabelProvider defaultLabelProvider)
		{
			this.columnIndex = columnIndex;
			direction = SWT.UP;
			labelProvider = defaultLabelProvider;
		}

		public int getDirection()
		{
			return direction;
		}

		/** Called when click on table header, reverse order */
		public void setColumn(int column)
		{
			if (column == columnIndex)
			{
				// Same column as last sort; toggle the direction
				direction = (direction == SWT.UP) ? SWT.DOWN : SWT.UP;
			} else
			{
				// New column; do a descending sort
				columnIndex = column;
				direction = SWT.DOWN;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2)
		{
			// Compare the text from label provider.
			String lp1 = labelProvider.getText(e1);
			String lp2 = labelProvider.getText(e2);
			String s1 = lp1 == null ? "" : lp1.toLowerCase();
			String s2 = lp2 == null ? "" : lp2.toLowerCase();
			int rc = s1.compareTo(s2);
			// If descending order, flip the direction
			return (direction == SWT.DOWN) ? -rc : rc;
		}

		public void setLabelProvider(ILabelProvider textProvider)
		{
			labelProvider = textProvider;
		}

	}