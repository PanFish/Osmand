package net.osmand.plus.activities;

import gnu.trove.list.array.TIntArrayList;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

public class SelectedGPXFragment extends OsmandExpandableListFragment {

	public static final int SEARCH_ID = -1;
//	private SearchView searchView;
	private OsmandApplication app;
	private GpxSelectionHelper selectedGpxHelper;
	private SelectedGPXAdapter adapter;
	private boolean lightContent;
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		app = (OsmandApplication) activity.getApplication();
		selectedGpxHelper = app.getSelectedGpxHelper();

		adapter = new SelectedGPXAdapter();
		setAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		lightContent = app.getSettings().isLightContent();
		adapter.setDisplayGroups(selectedGpxHelper.getDisplayGroups());
		selectedGpxHelper.setUiListener(new Runnable() {
			
			@Override
			public void run() {
				adapter.setDisplayGroups(selectedGpxHelper.getDisplayGroups());				
			}
		});
	}
	
	@Override
	public void onPause() {
		super.onPause();
		selectedGpxHelper.setUiListener(null);
	}


	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.export_fav, R.drawable.ic_action_search_light,
//				R.drawable.ic_action_search_dark, MenuItem.SHOW_AS_ACTION_ALWAYS
//						| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
//		searchView = new com.actionbarsherlock.widget.SearchView(getActivity());
//		mi.setActionView(searchView);
//		searchView.setOnQueryTextListener(new OnQueryTextListener() {
//
//			@Override
//			public boolean onQueryTextSubmit(String query) {
//				return true;
//			}
//
//			@Override
//			public boolean onQueryTextChange(String newText) {
//				return true;
//			}
//		});
//		createMenuItem(menu, ACTION_ID, R.string.export_fav, R.drawable.ic_action_gsave_light,
//				R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	public void showProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
	}

	private void selectSplitDistance(final GpxDisplayGroup model) {
		Builder bld = new AlertDialog.Builder(getActivity());
		int checkedItem = -1;
		List<String> options = new ArrayList<String>();
		final TIntArrayList distanceSplit = new TIntArrayList();
		final TIntArrayList timeSplit = new TIntArrayList();
		bld.setTitle(R.string.gpx_split_interval);
		options.add(app.getString(R.string.none));
		distanceSplit.add(-1);
		timeSplit.add(-1);
		addOptionSplit(50, true, options, distanceSplit, timeSplit);
		addOptionSplit(100, true, options, distanceSplit, timeSplit);
		addOptionSplit(200, true, options, distanceSplit, timeSplit);
		addOptionSplit(500, true, options, distanceSplit, timeSplit);
		addOptionSplit(1000, true, options, distanceSplit, timeSplit);
		addOptionSplit(2000, true, options, distanceSplit, timeSplit);
		addOptionSplit(5000, true, options, distanceSplit, timeSplit);
		addOptionSplit(15, false, options, distanceSplit, timeSplit);
		addOptionSplit(30, false, options, distanceSplit, timeSplit);
		addOptionSplit(60, false, options, distanceSplit, timeSplit);
		addOptionSplit(120, false, options, distanceSplit, timeSplit);
		addOptionSplit(150, false, options, distanceSplit, timeSplit);
		addOptionSplit(300, false, options, distanceSplit, timeSplit);
		addOptionSplit(600, false, options, distanceSplit, timeSplit);
		addOptionSplit(900, false, options, distanceSplit, timeSplit);
		
		bld.setSingleChoiceItems(options.toArray(new String[options.size()]), checkedItem, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0) {
					model.noSplit(app);
				} else if(distanceSplit.get(which) > 0) {
					model.splitByDistance(app, distanceSplit.get(which));
				} else if(timeSplit.get(which) > 0) {
					model.splitByTime(app, timeSplit.get(which));
				}
				adapter.notifyDataSetChanged();
				dialog.dismiss();
			}
		});
		bld.show();
		
	}

	private void addOptionSplit(int value, boolean distance, List<String> options, TIntArrayList distanceSplit,
			TIntArrayList timeSplit) {
		if(distance) {
			options.add(OsmAndFormatter.getFormattedDistance(value, app));
			distanceSplit.add(value);
			timeSplit.add(-1);
		} else {
			if(value < 60) {
				options.add(value+ " " + app.getString(R.string.int_seconds));
			} else if(value % 60 == 0){
				options.add((value/60) + " " + app.getString(R.string.int_min));
			} else {
				options.add((value/60f) + " " + app.getString(R.string.int_min));
			}
			distanceSplit.add(-1);
			timeSplit.add(value);
		}
		
	}

	class SelectedGPXAdapter extends OsmandBaseExpandableListAdapter  {

		Filter myFilter;
		private List<GpxDisplayGroup> displayGroups = new ArrayList<GpxDisplayGroup>();
		
		public void setDisplayGroups(List<GpxDisplayGroup> displayGroups) {
			this.displayGroups = displayGroups;
			notifyDataSetChanged();
		}


		@Override
		public GpxDisplayItem getChild(int groupPosition, int childPosition) {
			GpxDisplayGroup group = getGroup(groupPosition);
			return group.getModifiableList().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).getModifiableList().size();
		}

		@Override
		public GpxDisplayGroup getGroup(int groupPosition) {
			return displayGroups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return displayGroups.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.expandable_list_item_category_btn, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final GpxDisplayGroup model = getGroup(groupPosition);
			label.setText(model.getGroupName());
			final ImageView ch = (ImageView) row.findViewById(R.id.check_item);
			if(model.getType() != GpxDisplayItemType.TRACK_SEGMENT) {
				ch.setVisibility(View.INVISIBLE);
			} else {
				ch.setVisibility(View.VISIBLE);
				ch.setImageDrawable(getActivity().getResources().getDrawable(
						app.getSettings().isLightContent() ? R.drawable.ic_action_settings_light
								: R.drawable.ic_action_settings_dark));
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						selectSplitDistance(model);
					}

				});
			}
			return row;
		}
		

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.gpx_item_list_item, parent, false);
			}
			GpxDisplayItem child = getChild(groupPosition, childPosition);
			TextView label = (TextView) row.findViewById(R.id.name);
			TextView description = (TextView) row.findViewById(R.id.description);
			TextView additional = (TextView) row.findViewById(R.id.additional);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			if(child.splitMetric >= 0) {
				additional.setVisibility(View.VISIBLE);
				icon.setVisibility(View.INVISIBLE);
				if(child.group.isSplitDistance()) {
					additional.setText(OsmAndFormatter.getFormattedDistance(child.splitMetric, app));
				} else{
					additional.setText(Algorithms.formatDuration(child.splitMetric));
				}
			} else {
				icon.setVisibility(View.VISIBLE);
				additional.setVisibility(View.INVISIBLE);
				if (child.group.getType() == GpxDisplayItemType.TRACK_SEGMENT) {
					icon.setImageResource(!lightContent ? R.drawable.ic_action_polygom_dark
							: R.drawable.ic_action_polygom_light);
				} else if (child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
					icon.setImageResource(!lightContent ? R.drawable.ic_action_markers_dark
							: R.drawable.ic_action_markers_light);
				} else {
					icon.setImageResource(R.drawable.list_favorite);
				}
			}
			row.setTag(child);
				
			label.setText(Html.fromHtml(child.name.replace("\n", "<br/>")));
			if(child.expanded) {
				description.setText(Html.fromHtml(child.description));
				description.setVisibility(View.VISIBLE);
			} else {
				description.setVisibility(View.GONE);
			}

			return row;
		}

	}


	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		GpxDisplayItem child = adapter.getChild(groupPosition, childPosition);
		child.expanded = !child.expanded;
		adapter.notifyDataSetInvalidated();
		return true;
	}

	
}
