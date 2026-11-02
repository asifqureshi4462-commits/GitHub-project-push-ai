package com.asifqureshi.gitforge;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.data.local.SecurePreferences;
import com.asifqureshi.gitforge.data.local.UploadHistoryEntity;
import com.asifqureshi.gitforge.model.Branch;
import com.asifqureshi.gitforge.model.DeviceCodeResponse;
import com.asifqureshi.gitforge.model.FileItem;
import com.asifqureshi.gitforge.model.Repository;
import com.asifqureshi.gitforge.model.User;
import com.asifqureshi.gitforge.ui.adapter.BranchAdapter;
import com.asifqureshi.gitforge.ui.adapter.FilePreviewAdapter;
import com.asifqureshi.gitforge.ui.adapter.HistoryAdapter;
import com.asifqureshi.gitforge.ui.adapter.RepoAdapter;
import com.asifqureshi.gitforge.util.FileUtils;
import com.asifqureshi.gitforge.util.ZipUtils;
import com.asifqureshi.gitforge.viewmodel.MainViewModel;
import com.example.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private SecurePreferences securePreferences;

    // Navigation and Tab Views
    private View includeDashboard, includeRepos, includePusher, includeAiTools, includeSettingsHistory;
    private BottomNavigationView bottomNavigation;
    private TextView tvHeaderStatusPill;

    // Overlays
    private View overlayOnboarding, overlayLogin;

    // Adapters
    private RepoAdapter repoAdapter;
    private BranchAdapter branchAdapter;
    private FilePreviewAdapter filePreviewAdapter;
    private HistoryAdapter recentHistoryAdapter;
    private HistoryAdapter fullHistoryAdapter;

    // File Pickers (SAF)
    private ActivityResultLauncher<String> zipPickerLauncher;
    private ActivityResultLauncher<Uri> folderPickerLauncher;
    private ActivityResultLauncher<String> filesPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        securePreferences = viewModel.getSecurePreferences();

        // Apply saved theme
        applySavedTheme(securePreferences.getThemeMode());

        setContentView(R.layout.activity_main);

        initViews();
        setupAdapters();
        setupFilePickers();
        observeViewModel();
        setupListeners();

        // Check Onboarding & Login state
        if (!securePreferences.isOnboardingCompleted()) {
            overlayOnboarding.setVisibility(View.VISIBLE);
        } else if (!securePreferences.hasValidToken()) {
            overlayLogin.setVisibility(View.VISIBLE);
        } else {
            viewModel.loadRepositories();
        }
    }

    private void applySavedTheme(int mode) {
        if (mode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (mode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void initViews() {
        tvHeaderStatusPill = findViewById(R.id.tvHeaderStatusPill);
        includeDashboard = findViewById(R.id.includeDashboard);
        includeRepos = findViewById(R.id.includeRepos);
        includePusher = findViewById(R.id.includePusher);
        includeAiTools = findViewById(R.id.includeAiTools);
        includeSettingsHistory = findViewById(R.id.includeSettingsHistory);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        overlayOnboarding = findViewById(R.id.overlayOnboarding);
        overlayLogin = findViewById(R.id.overlayLogin);
    }

    private void setupAdapters() {
        // Repos
        RecyclerView rvRepos = includeRepos.findViewById(R.id.rvRepositories);
        rvRepos.setLayoutManager(new LinearLayoutManager(this));
        repoAdapter = new RepoAdapter();
        rvRepos.setAdapter(repoAdapter);

        // Branches
        RecyclerView rvBranches = includeRepos.findViewById(R.id.rvBranches);
        rvBranches.setLayoutManager(new LinearLayoutManager(this));
        branchAdapter = new BranchAdapter();
        rvBranches.setAdapter(branchAdapter);

        // File Preview
        RecyclerView rvFilePreview = includePusher.findViewById(R.id.rvFilePreview);
        rvFilePreview.setLayoutManager(new LinearLayoutManager(this));
        filePreviewAdapter = new FilePreviewAdapter();
        rvFilePreview.setAdapter(filePreviewAdapter);

        // Recent History on Dashboard
        RecyclerView rvRecent = includeDashboard.findViewById(R.id.rvRecentHistory);
        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        recentHistoryAdapter = new HistoryAdapter();
        rvRecent.setAdapter(recentHistoryAdapter);

        // Full History in Settings
        RecyclerView rvFull = includeSettingsHistory.findViewById(R.id.rvFullHistory);
        rvFull.setLayoutManager(new LinearLayoutManager(this));
        fullHistoryAdapter = new HistoryAdapter();
        rvFull.setAdapter(fullHistoryAdapter);
    }

    private void setupFilePickers() {
        // ZIP Picker
        zipPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            parseZipUri(uri);
                        }
                    }
                }
        );

        // Folder Picker via Storage Access Framework
        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            parseFolderUri(uri);
                        }
                    }
                }
        );

        // Multiple Files Picker
        filesPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                new ActivityResultCallback<List<Uri>>() {
                    @Override
                    public void onActivityResult(List<Uri> uris) {
                        if (uris != null && !uris.isEmpty()) {
                            parseMultipleFiles(uris);
                        }
                    }
                }
        );
    }

    private void parseZipUri(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        final List<FileItem> parsed = ZipUtils.parseZipStream(is);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewModel.setSelectedFiles(parsed);
                                Toast.makeText(MainActivity.this, "Loaded " + parsed.size() + " files from ZIP archive", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error parsing ZIP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void parseFolderUri(final Uri treeUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DocumentFile dir = DocumentFile.fromTreeUri(MainActivity.this, treeUri);
                    if (dir != null) {
                        final List<FileItem> items = FileUtils.traverseDocumentFolder(MainActivity.this, dir, "");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewModel.setSelectedFiles(items);
                                Toast.makeText(MainActivity.this, "Loaded " + items.size() + " files from folder", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Folder read error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void parseMultipleFiles(final List<Uri> uris) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<FileItem> items = new ArrayList<>();
                for (Uri u : uris) {
                    try {
                        String name = FileUtils.getFileName(MainActivity.this, u);
                        byte[] data = FileUtils.readBytes(MainActivity.this, u);
                        items.add(new FileItem(name, name, data.length, false, data));
                    } catch (Exception ignored) {}
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewModel.setSelectedFiles(items);
                        Toast.makeText(MainActivity.this, "Loaded " + items.size() + " files", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void observeViewModel() {
        // Authenticated user observer
        viewModel.getCurrentUser().observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null) {
                    tvHeaderStatusPill.setText("@" + user.getLogin());
                    tvHeaderStatusPill.setTextColor(0xFF10B981); // Green

                    TextView tvUserWelcome = includeDashboard.findViewById(R.id.tvUserWelcome);
                    TextView tvUserLogin = includeDashboard.findViewById(R.id.tvUserLogin);
                    tvUserWelcome.setText("Welcome, " + user.getName());
                    tvUserLogin.setText("@" + user.getLogin());

                    TextView tvSettingsUserStatus = includeSettingsHistory.findViewById(R.id.tvSettingsUserStatus);
                    tvSettingsUserStatus.setText("Connected as: @" + user.getLogin() + " (" + user.getName() + ")");
                } else {
                    tvHeaderStatusPill.setText("Offline");
                    tvHeaderStatusPill.setTextColor(0xFFF59E0B); // Amber

                    TextView tvSettingsUserStatus = includeSettingsHistory.findViewById(R.id.tvSettingsUserStatus);
                    tvSettingsUserStatus.setText("Not connected to GitHub");
                }
            }
        });

        // Repositories observer
        viewModel.getRepositories().observe(this, new Observer<List<Repository>>() {
            @Override
            public void onChanged(List<Repository> repositories) {
                repoAdapter.setRepositories(repositories);
                TextView tvEmptyRepos = includeRepos.findViewById(R.id.tvEmptyRepos);
                if (repositories == null || repositories.isEmpty()) {
                    tvEmptyRepos.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyRepos.setVisibility(View.GONE);
                }

                TextView tvStatRepos = includeDashboard.findViewById(R.id.tvStatRepos);
                tvStatRepos.setText(String.valueOf(repositories != null ? repositories.size() : 0));
            }
        });

        // Selected Repository observer
        viewModel.getSelectedRepository().observe(this, new Observer<Repository>() {
            @Override
            public void onChanged(Repository repository) {
                if (repository != null) {
                    repoAdapter.setSelectedRepoId(repository.getId());

                    // Update banner in repos view
                    View banner = includeRepos.findViewById(R.id.cardSelectedRepoBanner);
                    banner.setVisibility(View.VISIBLE);
                    TextView tvName = includeRepos.findViewById(R.id.tvSelectedRepoName);
                    tvName.setText(repository.getFullName());

                    // Update pusher destination
                    TextView tvPusherRepo = includePusher.findViewById(R.id.tvPusherTargetRepo);
                    tvPusherRepo.setText(repository.getFullName());
                }
            }
        });

        // Selected Branch observer
        viewModel.getSelectedBranch().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String branchName) {
                if (branchName != null) {
                    TextView tvBranch = includeRepos.findViewById(R.id.tvSelectedBranchName);
                    tvBranch.setText("Target: " + branchName);

                    TextView tvPusherBranch = includePusher.findViewById(R.id.tvPusherTargetBranch);
                    tvPusherBranch.setText("Branch: " + branchName);
                }
            }
        });

        // Branches list observer
        viewModel.getBranches().observe(this, new Observer<List<Branch>>() {
            @Override
            public void onChanged(List<Branch> branches) {
                branchAdapter.setBranches(branches, viewModel.getSelectedBranch().getValue());
            }
        });

        // Selected Files observer
        viewModel.getSelectedFiles().observe(this, new Observer<List<FileItem>>() {
            @Override
            public void onChanged(List<FileItem> fileItems) {
                filePreviewAdapter.setFiles(fileItems);
                View cardPreview = includePusher.findViewById(R.id.cardPreviewSection);

                if (fileItems != null && !fileItems.isEmpty()) {
                    cardPreview.setVisibility(View.VISIBLE);
                    long totalSize = 0;
                    int selectedCount = 0;
                    for (FileItem f : fileItems) {
                        if (f.isSelected()) {
                            totalSize += f.getSizeBytes();
                            selectedCount++;
                        }
                    }
                    TextView tvCountSize = includePusher.findViewById(R.id.tvPreviewCountSize);
                    tvCountSize.setText(selectedCount + "/" + fileItems.size() + " files • " + FileUtils.formatSize(totalSize));
                } else {
                    cardPreview.setVisibility(View.GONE);
                }
            }
        });

        // Upload Progress observer
        viewModel.getUploadProgress().observe(this, new Observer<MainViewModel.UploadProgressState>() {
            @Override
            public void onChanged(MainViewModel.UploadProgressState state) {
                View cardProgress = includePusher.findViewById(R.id.cardUploadProgress);
                if (state != null && state.isRunning) {
                    cardProgress.setVisibility(View.VISIBLE);
                    TextView tvStep = includePusher.findViewById(R.id.tvProgressStepTitle);
                    TextView tvPercent = includePusher.findViewById(R.id.tvProgressPercent);
                    TextView tvDetail = includePusher.findViewById(R.id.tvProgressItemDetail);
                    LinearProgressIndicator bar = includePusher.findViewById(R.id.progressUploadBar);

                    tvStep.setText(state.step);
                    tvPercent.setText(state.percent + "%");
                    tvDetail.setText("Processing: " + state.currentItem + " of " + state.totalItems);
                    bar.setProgress(state.percent);
                } else {
                    cardProgress.setVisibility(View.GONE);
                }
            }
        });

        // Upload Result observer
        viewModel.getUploadResult().observe(this, new Observer<MainViewModel.UploadResultState>() {
            @Override
            public void onChanged(final MainViewModel.UploadResultState result) {
                View cardResult = includePusher.findViewById(R.id.cardUploadResult);
                if (result != null) {
                    cardResult.setVisibility(View.VISIBLE);
                    ImageView ivIcon = includePusher.findViewById(R.id.ivResultIcon);
                    TextView tvTitle = includePusher.findViewById(R.id.tvResultTitle);
                    TextView tvDesc = includePusher.findViewById(R.id.tvResultDescription);
                    TextView tvSha = includePusher.findViewById(R.id.tvResultCommitSha);
                    MaterialButton btnViewOnGitHub = includePusher.findViewById(R.id.btnViewOnGitHub);

                    if (result.isSuccess) {
                        ivIcon.setImageResource(R.drawable.ic_check_circle);
                        ivIcon.setColorFilter(0xFF10B981);
                        tvTitle.setText(R.string.upload_success_title);
                        tvTitle.setTextColor(0xFFF8FAFC);
                        tvDesc.setText("Pushed " + result.totalFiles + " files successfully to GitHub.");
                        tvSha.setText("Commit SHA: " + result.commitSha);
                        tvSha.setVisibility(View.VISIBLE);
                        btnViewOnGitHub.setVisibility(View.VISIBLE);

                        btnViewOnGitHub.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (result.htmlUrl != null && !result.htmlUrl.isEmpty()) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.htmlUrl));
                                    startActivity(browserIntent);
                                }
                            }
                        });
                    } else {
                        ivIcon.setImageResource(R.drawable.ic_error);
                        ivIcon.setColorFilter(0xFFEF4444);
                        tvTitle.setText(R.string.upload_failed_title);
                        tvTitle.setTextColor(0xFFEF4444);
                        tvDesc.setText(result.errorMessage != null ? result.errorMessage : "An error occurred during upload.");
                        tvSha.setVisibility(View.GONE);
                        btnViewOnGitHub.setVisibility(View.GONE);
                    }
                } else {
                    cardResult.setVisibility(View.GONE);
                }
            }
        });

        // Room Upload History observer
        viewModel.getRecentHistory().observe(this, new Observer<List<UploadHistoryEntity>>() {
            @Override
            public void onChanged(List<UploadHistoryEntity> list) {
                recentHistoryAdapter.setHistoryList(list);
                TextView tvEmpty = includeDashboard.findViewById(R.id.tvEmptyRecentHistory);
                tvEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getAllHistory().observe(this, new Observer<List<UploadHistoryEntity>>() {
            @Override
            public void onChanged(List<UploadHistoryEntity> list) {
                fullHistoryAdapter.setHistoryList(list);
                TextView tvEmpty = includeSettingsHistory.findViewById(R.id.tvEmptyFullHistory);
                tvEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getTotalUploads().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer total) {
                TextView tv = includeDashboard.findViewById(R.id.tvStatUploads);
                tv.setText(String.valueOf(total != null ? total : 0));
            }
        });

        viewModel.getSuccessUploads().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer success) {
                Integer total = viewModel.getTotalUploads().getValue();
                TextView tv = includeDashboard.findViewById(R.id.tvStatSuccess);
                if (total != null && total > 0 && success != null) {
                    int rate = (int) ((success / (float) total) * 100);
                    tv.setText(rate + "%");
                } else {
                    tv.setText("100%");
                }
            }
        });

        // AI Result observer
        viewModel.getAiResult().observe(this, new Observer<MainViewModel.AiResultState>() {
            @Override
            public void onChanged(final MainViewModel.AiResultState state) {
                View cardOutput = includeAiTools.findViewById(R.id.cardAiOutput);
                ProgressBar pbLoading = includeAiTools.findViewById(R.id.pbAiLoading);
                TextView tvOutput = includeAiTools.findViewById(R.id.tvAiOutputText);
                MaterialButton btnAttach = includeAiTools.findViewById(R.id.btnAttachReadmeToUpload);

                if (state != null) {
                    pbLoading.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);
                    if (!state.isLoading) {
                        cardOutput.setVisibility(View.VISIBLE);
                        if (state.output != null) {
                            tvOutput.setText(state.output);
                            tvOutput.setTextColor(0xFFF8FAFC);
                            btnAttach.setVisibility(state.toolType == 1 ? View.VISIBLE : View.GONE);
                        } else if (state.error != null) {
                            tvOutput.setText(state.error);
                            tvOutput.setTextColor(0xFFEF4444);
                            btnAttach.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });

        // Device Code observer
        viewModel.getDeviceCode().observe(this, new Observer<DeviceCodeResponse>() {
            @Override
            public void onChanged(final DeviceCodeResponse code) {
                if (code != null) {
                    LinearLayout layoutBox = overlayLogin.findViewById(R.id.layoutDeviceCodeBox);
                    layoutBox.setVisibility(View.VISIBLE);
                    TextView tvCode = overlayLogin.findViewById(R.id.tvDeviceCodeValue);
                    tvCode.setText(code.getUserCode());

                    MaterialButton btnOpen = overlayLogin.findViewById(R.id.btnOpenDeviceUrl);
                    btnOpen.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(code.getVerificationUri())));
                        }
                    });
                }
            }
        });

        // Auth Error observer
        viewModel.getAuthError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String err) {
                TextView tvErr = overlayLogin.findViewById(R.id.tvLoginError);
                if (err != null && !err.isEmpty()) {
                    tvErr.setText(err);
                    tvErr.setVisibility(View.VISIBLE);
                } else {
                    tvErr.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupListeners() {
        // Bottom Navigation Tab Switching
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                showTab(id);
                return true;
            }
        });

        // Onboarding Buttons
        MaterialButton btnOnboardingAction = overlayOnboarding.findViewById(R.id.btnOnboardingAction);
        btnOnboardingAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                securePreferences.setOnboardingCompleted(true);
                overlayOnboarding.setVisibility(View.GONE);
                if (!securePreferences.hasValidToken()) {
                    overlayLogin.setVisibility(View.VISIBLE);
                }
            }
        });

        TextView btnOnboardingSkip = overlayOnboarding.findViewById(R.id.btnOnboardingSkip);
        btnOnboardingSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                securePreferences.setOnboardingCompleted(true);
                overlayOnboarding.setVisibility(View.GONE);
            }
        });

        // Login Overlay Buttons
        final EditText etLoginToken = overlayLogin.findViewById(R.id.etLoginToken);
        MaterialButton btnLoginConnect = overlayLogin.findViewById(R.id.btnLoginConnect);
        btnLoginConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = etLoginToken.getText() != null ? etLoginToken.getText().toString().trim() : "";
                if (token.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a valid GitHub token", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.loginWithToken(token);
                overlayLogin.setVisibility(View.GONE);
            }
        });

        MaterialButton btnLoginDeviceFlow = overlayLogin.findViewById(R.id.btnLoginDeviceFlow);
        btnLoginDeviceFlow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Initiates GitHub Device Flow with standard OAuth client
                viewModel.startDeviceOAuth("0123456789abcdef0123");
            }
        });

        TextView btnLoginCancel = overlayLogin.findViewById(R.id.btnLoginCancel);
        btnLoginCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayLogin.setVisibility(View.GONE);
            }
        });

        // Dashboard Buttons
        includeDashboard.findViewById(R.id.btnQuickConnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayLogin.setVisibility(View.VISIBLE);
            }
        });

        includeDashboard.findViewById(R.id.btnHeroStartPush).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.nav_pusher);
            }
        });

        includeDashboard.findViewById(R.id.btnViewAllHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.nav_settings);
            }
        });

        // Quick AI Launchers on Dashboard
        includeDashboard.findViewById(R.id.cardQuickReadme).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.nav_ai_tools);
                MaterialButtonToggleGroup group = includeAiTools.findViewById(R.id.toggleAiTools);
                group.check(R.id.tabReadme);
            }
        });

        includeDashboard.findViewById(R.id.cardQuickReview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.nav_ai_tools);
                MaterialButtonToggleGroup group = includeAiTools.findViewById(R.id.toggleAiTools);
                group.check(R.id.tabReview);
            }
        });

        includeDashboard.findViewById(R.id.cardQuickError).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.nav_ai_tools);
                MaterialButtonToggleGroup group = includeAiTools.findViewById(R.id.toggleAiTools);
                group.check(R.id.tabError);
            }
        });

        // Repos Search
        EditText etSearchRepos = includeRepos.findViewById(R.id.etSearchRepos);
        etSearchRepos.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchRepositories(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Repo selection in list
        repoAdapter.setOnRepoClickListener(new RepoAdapter.OnRepoClickListener() {
            @Override
            public void onRepoClick(Repository repository) {
                viewModel.selectRepository(repository);
                Toast.makeText(MainActivity.this, "Selected: " + repository.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Branch Sheet Toggles
        includeRepos.findViewById(R.id.btnChangeBranch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View card = includeRepos.findViewById(R.id.cardBranchSelector);
                card.setVisibility(card.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        includeRepos.findViewById(R.id.btnCloseBranchSelector).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                includeRepos.findViewById(R.id.cardBranchSelector).setVisibility(View.GONE);
            }
        });

        branchAdapter.setOnBranchClickListener(new BranchAdapter.OnBranchClickListener() {
            @Override
            public void onBranchClick(Branch branch) {
                viewModel.selectBranch(branch.getName());
                includeRepos.findViewById(R.id.cardBranchSelector).setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Target branch set to " + branch.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Pusher Screen Listeners
        includePusher.findViewById(R.id.btnPusherPickRepo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.nav_repos);
            }
        });

        includePusher.findViewById(R.id.btnPickZip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zipPickerLauncher.launch("application/zip");
            }
        });

        includePusher.findViewById(R.id.btnPickFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                folderPickerLauncher.launch(null);
            }
        });

        includePusher.findViewById(R.id.btnPickFiles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filesPickerLauncher.launch("*/*");
            }
        });

        // File preview modal
        filePreviewAdapter.setListener(new FilePreviewAdapter.OnFileItemActionListener() {
            @Override
            public void onFilePreviewRequested(FileItem item) {
                showFilePreviewDialog(item);
            }

            @Override
            public void onFileSelectionChanged(int position, boolean isSelected) {
                viewModel.toggleFileSelection(position);
            }
        });

        // Quick Commit Chips
        final EditText etCommitMsg = includePusher.findViewById(R.id.etCommitMessage);
        includePusher.findViewById(R.id.chipInitialCommit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etCommitMsg.setText("Initial commit via GitForge Pusher");
            }
        });
        includePusher.findViewById(R.id.chipUpdateFiles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etCommitMsg.setText("Update project source files via GitForge");
            }
        });
        includePusher.findViewById(R.id.chipAddFeature).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etCommitMsg.setText("feat: add project files and assets");
            }
        });

        // Execute Push Button
        includePusher.findViewById(R.id.btnExecutePush).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = etCommitMsg.getText() != null ? etCommitMsg.getText().toString().trim() : "";
                if (msg.isEmpty()) {
                    msg = "Upload project files via GitForge Pusher";
                }
                CheckBox cb = includePusher.findViewById(R.id.cbCreateBranch);
                boolean createBranch = cb.isChecked();
                viewModel.startUpload(msg, createBranch);
            }
        });

        includePusher.findViewById(R.id.btnDismissResult).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                includePusher.findViewById(R.id.cardUploadResult).setVisibility(View.GONE);
            }
        });

        // AI Tools Listeners
        final MaterialButtonToggleGroup toggleAi = includeAiTools.findViewById(R.id.toggleAiTools);
        final TextView tvAiTitle = includeAiTools.findViewById(R.id.tvAiToolTitle);
        final TextView tvAiDesc = includeAiTools.findViewById(R.id.tvAiToolDesc);
        final EditText etAiInput = includeAiTools.findViewById(R.id.etAiInput);
        final MaterialButton btnRunAi = includeAiTools.findViewById(R.id.btnRunAi);

        toggleAi.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked) return;
                if (checkedId == R.id.tabReadme) {
                    tvAiTitle.setText(R.string.ai_readme_title);
                    tvAiDesc.setText(R.string.ai_readme_desc);
                    etAiInput.setHint("Project description, features, libraries used...");
                    btnRunAi.setText("Generate README.md");
                } else if (checkedId == R.id.tabReview) {
                    tvAiTitle.setText(R.string.ai_review_title);
                    tvAiDesc.setText(R.string.ai_review_desc);
                    etAiInput.setHint("Paste source code snippet here...");
                    btnRunAi.setText("Review Code");
                } else if (checkedId == R.id.tabError) {
                    tvAiTitle.setText(R.string.ai_error_title);
                    tvAiDesc.setText(R.string.ai_error_desc);
                    etAiInput.setHint("Paste Git/compiler/runtime error trace here...");
                    btnRunAi.setText("Explain & Fix Error");
                }
            }
        });
        toggleAi.check(R.id.tabReadme);

        btnRunAi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etAiInput.getText() != null ? etAiInput.getText().toString().trim() : "";
                if (input.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please provide input content for AI analysis", Toast.LENGTH_SHORT).show();
                    return;
                }
                int checked = toggleAi.getCheckedButtonId();
                if (checked == R.id.tabReadme) {
                    viewModel.runAiReadme(input);
                } else if (checked == R.id.tabReview) {
                    viewModel.runAiReview(input);
                } else if (checked == R.id.tabError) {
                    viewModel.runAiErrorExplain(input);
                }
            }
        });

        // Copy AI output
        includeAiTools.findViewById(R.id.btnCopyAiOutput).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tvOutput = includeAiTools.findViewById(R.id.tvAiOutputText);
                CharSequence text = tvOutput.getText();
                if (text != null && text.length() > 0) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("GitForge AI Output", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Output copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Attach AI README to Upload
        includeAiTools.findViewById(R.id.btnAttachReadmeToUpload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tvOutput = includeAiTools.findViewById(R.id.tvAiOutputText);
                String text = tvOutput.getText().toString();
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                FileItem readmeItem = new FileItem("README.md", "README.md", bytes.length, false, bytes);

                List<FileItem> current = viewModel.getSelectedFiles().getValue();
                List<FileItem> updated = current != null ? new ArrayList<>(current) : new ArrayList<FileItem>();

                // Remove existing README.md if present
                for (int i = updated.size() - 1; i >= 0; i--) {
                    if ("README.md".equalsIgnoreCase(updated.get(i).getRelativePath())) {
                        updated.remove(i);
                    }
                }
                updated.add(readmeItem);
                viewModel.setSelectedFiles(updated);

                Toast.makeText(MainActivity.this, "README.md added to current upload queue!", Toast.LENGTH_SHORT).show();
                bottomNavigation.setSelectedItemId(R.id.nav_pusher);
            }
        });

        // Settings Screen Listeners
        final EditText etSettingsToken = includeSettingsHistory.findViewById(R.id.etSettingsToken);
        includeSettingsHistory.findViewById(R.id.btnSettingsSaveToken).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = etSettingsToken.getText() != null ? etSettingsToken.getText().toString().trim() : "";
                if (!token.isEmpty()) {
                    viewModel.loginWithToken(token);
                    Toast.makeText(MainActivity.this, "Verifying GitHub credentials...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        includeSettingsHistory.findViewById(R.id.btnSettingsLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.logout();
                Toast.makeText(MainActivity.this, "Logged out of GitHub", Toast.LENGTH_SHORT).show();
            }
        });

        includeSettingsHistory.findViewById(R.id.btnSettingsDeviceFlow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayLogin.setVisibility(View.VISIBLE);
                viewModel.startDeviceOAuth("0123456789abcdef0123");
            }
        });

        // Theme Toggle in Settings
        MaterialButtonToggleGroup toggleTheme = includeSettingsHistory.findViewById(R.id.toggleTheme);
        int currentTheme = securePreferences.getThemeMode();
        if (currentTheme == 1) toggleTheme.check(R.id.btnThemeLight);
        else if (currentTheme == 2) toggleTheme.check(R.id.btnThemeDark);
        else toggleTheme.check(R.id.btnThemeSystem);

        toggleTheme.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked) return;
                int mode = 0;
                if (checkedId == R.id.btnThemeLight) mode = 1;
                else if (checkedId == R.id.btnThemeDark) mode = 2;
                securePreferences.setThemeMode(mode);
                applySavedTheme(mode);
            }
        });

        // AI Provider Settings
        final EditText etAiModel = includeSettingsHistory.findViewById(R.id.etAiModel);
        final EditText etAiApiKey = includeSettingsHistory.findViewById(R.id.etAiApiKey);
        etAiModel.setText(securePreferences.getAiModel());
        if (!securePreferences.getAiApiKey().isEmpty()) {
            etAiApiKey.setText(securePreferences.getAiApiKey());
        }

        includeSettingsHistory.findViewById(R.id.btnSaveAiConfig).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String model = etAiModel.getText() != null ? etAiModel.getText().toString().trim() : "gemini-1.5-flash";
                String key = etAiApiKey.getText() != null ? etAiApiKey.getText().toString().trim() : "";
                securePreferences.saveAiModel(model);
                securePreferences.saveAiApiKey(key);
                Toast.makeText(MainActivity.this, "AI configuration saved securely", Toast.LENGTH_SHORT).show();
            }
        });

        includeSettingsHistory.findViewById(R.id.btnClearHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.clearHistory();
                Toast.makeText(MainActivity.this, "Upload history cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTab(int tabId) {
        includeDashboard.setVisibility(tabId == R.id.nav_dashboard ? View.VISIBLE : View.GONE);
        includeRepos.setVisibility(tabId == R.id.nav_repos ? View.VISIBLE : View.GONE);
        includePusher.setVisibility(tabId == R.id.nav_pusher ? View.VISIBLE : View.GONE);
        includeAiTools.setVisibility(tabId == R.id.nav_ai_tools ? View.VISIBLE : View.GONE);
        includeSettingsHistory.setVisibility(tabId == R.id.nav_settings ? View.VISIBLE : View.GONE);
    }

    private void showFilePreviewDialog(final FileItem item) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_file_preview);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogFileTitle);
        TextView tvSize = dialog.findViewById(R.id.tvDialogFileSize);
        final TextView tvContent = dialog.findViewById(R.id.tvDialogFileContent);

        tvTitle.setText(item.getRelativePath());
        tvSize.setText("Size: " + item.getFormattedSize());

        final String previewText = FileUtils.getPreviewSnippet(item.getData(), 2000);
        tvContent.setText(previewText);

        dialog.findViewById(R.id.btnDialogClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.btnDialogCopyContent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("File Content", previewText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "File preview copied", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
