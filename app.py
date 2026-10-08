import os
import yt_dlp
import customtkinter as ctk
from tkinter import messagebox
from threading import Thread
from typing import List, Tuple, Dict
import webbrowser
import time

class YouTubeVideoDownloader:
    def __init__(self):
        self.videos: List[Tuple[str, str]] = []
        self.checkbox_vars: List[ctk.BooleanVar] = []
        self.quality_vars: Dict[int, ctk.StringVar] = {}
        self.quality_frames: Dict[int, ctk.CTkFrame] = {}
        self.create_main_window()

    def create_main_window(self):
        ctk.set_appearance_mode("dark")
        ctk.set_default_color_theme("blue")

        self.root = ctk.CTk()
        self.root.title("YouTube Video Downloader")
        self.root.geometry("800x600")
        self.root.configure(fg_color="#1a1a1a")

        self.create_widgets()
        self.root.mainloop()

    def create_widgets(self):
        main_container = ctk.CTkFrame(self.root, fg_color="transparent")
        main_container.pack(fill="both", expand=True, padx=30, pady=20)

        header_frame = ctk.CTkFrame(main_container, fg_color="#2d2d2d", corner_radius=15)
        header_frame.pack(fill="x", pady=(0, 20))
        
        welcome_label = ctk.CTkLabel(
            header_frame,
            text="YouTube Video Downloader",
            font=("Roboto", 24, "bold"),
            text_color="#ff4444"
        )
        welcome_label.pack(pady=20)

        url_frame = ctk.CTkFrame(main_container, fg_color="#2d2d2d", corner_radius=15)
        url_frame.pack(fill="x", pady=(0, 20))

        url_label = ctk.CTkLabel(
            url_frame,
            text="Enter Video, Playlist, or Channel URL:",
            font=("Roboto", 14),
            text_color="#ffffff"
        )
        url_label.pack(pady=(15, 5), padx=20)

        self.url_entry = ctk.CTkEntry(
            url_frame,
            width=600,
            height=40,
            placeholder_text="Paste YouTube URL here...",
            font=("Roboto", 12),
            corner_radius=8
        )
        self.url_entry.pack(pady=(0, 10), padx=20)

        list_videos_button = ctk.CTkButton(
            url_frame,
            text="List Video(s)/Qualities",
            command=self.list_videos_or_qualities,
            font=("Roboto", 12, "bold"),
            height=38,
            corner_radius=8,
            fg_color="#ff4444",
            hover_color="#cc3333"
        )
        list_videos_button.pack(pady=(0, 15))

        self.video_frame = ctk.CTkScrollableFrame(
            main_container,
            fg_color="#2d2d2d",
            corner_radius=15,
            height=300
        )
        self.video_frame.pack(fill="both", expand=True, pady=(0, 20))

        button_frame = ctk.CTkFrame(main_container, fg_color="transparent")
        button_frame.pack(fill="x", pady=(0, 10))

        self.download_button = ctk.CTkButton(
            button_frame,
            text="Download Selected Video(s)",
            command=self.start_download,
            font=("Roboto", 12, "bold"),
            height=40,
            corner_radius=8,
            fg_color="#00cc66",
            hover_color="#00994d"
        )
        self.download_button.pack(side='left', padx=5)

        contact_button = ctk.CTkButton(
            button_frame,
            text="Contact Us",
            command=self.open_contact_window,
            font=("Roboto", 12),
            height=40,
            corner_radius=8,
            fg_color="#3366ff",
            hover_color="#2952cc"
        )
        contact_button.pack(side='right', padx=5)

    def get_video_qualities(self, video_url: str) -> List[str]:
        ydl_opts = {'quiet': True}
        available_resolutions = ['144p', '240p', '360p', '480p', '720p', '1080p']
        quality_options = []

        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                video_info = ydl.extract_info(video_url, download=False)
                for fmt in video_info['formats']:
                    if 'format_note' in fmt and fmt['format_note'] in available_resolutions:
                        if fmt['format_note'] not in quality_options:
                            quality_options.append(fmt['format_note'])

            quality_options.sort(key=lambda x: available_resolutions.index(x))
            return quality_options
        except Exception as e:
            print(f"Error getting qualities: {e}")
            return ['720p']

    def toggle_quality_selection(self, index: int, checkbox_var: ctk.BooleanVar, container_frame: ctk.CTkFrame):
        if checkbox_var.get():
            qualities = self.get_video_qualities(self.videos[index][1])
            
            quality_frame = ctk.CTkFrame(container_frame, fg_color="#363636", corner_radius=8)
            quality_frame.pack(fill="x", padx=30, pady=(5, 10))
            self.quality_frames[index] = quality_frame
            
            quality_var = ctk.StringVar()
            self.quality_vars[index] = quality_var
            
            default_quality = '720p' if '720p' in qualities else qualities[-1]
            quality_var.set(default_quality)
            
            quality_label = ctk.CTkLabel(
                quality_frame,
                text="Select Quality:",
                font=("Roboto", 12),
                text_color="#888888"
            )
            quality_label.pack(anchor='w', padx=10, pady=(5, 0))
            
            options_frame = ctk.CTkFrame(quality_frame, fg_color="transparent")
            options_frame.pack(fill="x", padx=10, pady=5)
            
            for quality in qualities:
                radio = ctk.CTkRadioButton(
                    options_frame,
                    text=quality,
                    variable=quality_var,
                    value=quality,
                    font=("Roboto", 11),
                    fg_color="#ff4444",
                    border_color="#666666"
                )
                radio.pack(side='left', padx=5, pady=5)
        else:
            if index in self.quality_frames:
                self.quality_frames[index].destroy()
                del self.quality_frames[index]
                if index in self.quality_vars:
                    del self.quality_vars[index]

    def display_videos(self):
        for i, (title, _) in enumerate(self.videos):
            video_container = ctk.CTkFrame(self.video_frame, fg_color="#363636", corner_radius=10)
            video_container.pack(fill="x", padx=10, pady=5)
            
            checkbox_frame = ctk.CTkFrame(video_container, fg_color="transparent")
            checkbox_frame.pack(fill="x", padx=10, pady=10)
            
            var = ctk.BooleanVar()
            self.checkbox_vars.append(var)
            
            cb = ctk.CTkCheckBox(
                checkbox_frame,
                text=title,
                variable=var,
                font=("Roboto", 12),
                fg_color="#ff4444",
                border_color="#666666",
                corner_radius=6,
                command=lambda idx=i, v=var, c=video_container: self.toggle_quality_selection(idx, v, c)
            )
            cb.pack(anchor='w')

    def display_video_qualities(self, video_title, video_url):
        video_container = ctk.CTkFrame(self.video_frame, fg_color="#363636", corner_radius=10)
        video_container.pack(fill="x", padx=10, pady=5)
        
        title_label = ctk.CTkLabel(
            video_container,
            text=video_title,
            font=("Roboto", 14, "bold"),
            text_color="#ffffff"
        )
        title_label.pack(anchor='w', padx=15, pady=10)

        qualities = self.get_video_qualities(video_url)
        
        quality_frame = ctk.CTkFrame(video_container, fg_color="transparent")
        quality_frame.pack(fill="x", padx=15, pady=(0, 10))
        
        self.selected_quality = ctk.StringVar()
        default_quality = '720p' if '720p' in qualities else qualities[-1]
        self.selected_quality.set(default_quality)
        
        for quality in qualities:
            radio_button = ctk.CTkRadioButton(
                quality_frame,
                text=quality,
                variable=self.selected_quality,
                value=quality,
                font=("Roboto", 11),
                fg_color="#ff4444",
                border_color="#666666"
            )
            radio_button.pack(side='left', padx=5)

        self.videos = [(video_title, video_url)]

    def create_progress_window(self):
        self.progress_window = ctk.CTkToplevel(self.root)
        self.progress_window.title("Download Progress")
        self.progress_window.geometry("500x300")
        self.progress_window.configure(fg_color="#1a1a1a")

        progress_frame = ctk.CTkFrame(self.progress_window, fg_color="#2d2d2d", corner_radius=15)
        progress_frame.pack(fill="both", expand=True, padx=20, pady=20)

        self.file_name_label = ctk.CTkLabel(
            progress_frame,
            text="",
            font=("Roboto", 12),
            text_color="#ffffff"
        )
        self.file_name_label.pack(pady=(20, 10), padx=20, anchor='w')

        self.progress_bar = ctk.CTkProgressBar(
            progress_frame,
            width=460,
            height=15,
            corner_radius=10,
            fg_color="#363636",
            progress_color="#ff4444"
        )
        self.progress_bar.pack(pady=(0, 10), padx=20)
        self.progress_bar.set(0)

        details_frame = ctk.CTkFrame(progress_frame, fg_color="transparent")
        details_frame.pack(fill='x', padx=20)

        self.size_label = ctk.CTkLabel(
            details_frame,
            text="0 MB / 0 MB",
            font=("Roboto", 12),
            text_color="#888888"
        )
        self.size_label.pack(side='left')

        self.speed_label = ctk.CTkLabel(
            details_frame,
            text="0 KB/s",
            font=("Roboto", 12),
            text_color="#888888"
        )
        self.speed_label.pack(side='right')

        self.eta_label = ctk.CTkLabel(
            progress_frame,
            text="Time remaining: --:--",
            font=("Roboto", 12),
            text_color="#888888"
        )
        self.eta_label.pack(pady=(10, 20))

    def open_contact_window(self, event=None):
        contact_window = ctk.CTkToplevel(self.root)
        contact_window.title("Contact Us")
        contact_window.geometry("400x300")
        contact_window.configure(fg_color="#1a1a1a")

        contact_frame = ctk.CTkFrame(contact_window, fg_color="#2d2d2d", corner_radius=15)
        contact_frame.pack(fill="both", expand=True, padx=20, pady=20)

        title_label = ctk.CTkLabel(
            contact_frame,
            text="Get in Touch",
            font=("Roboto", 20, "bold"),
            text_color="#ff4444"
        )
        title_label.pack(pady=20)

        email_button = ctk.CTkButton(
            contact_frame,
            text="Email Us",
            command=lambda: webbrowser.open("mailto:contact.naeemasghar@gmail.com"),
            font=("Roboto", 12),
            height=40,
            corner_radius=8,
            fg_color="#ff4444",
            hover_color="#cc3333"
        )
        email_button.pack(pady=10)

        whatsapp_button = ctk.CTkButton(
            contact_frame,
            text="WhatsApp",
            command=lambda: webbrowser.open("https://wa.me/+923176344293"),
            font=("Roboto", 12),
            height=40,
            corner_radius=8,
            fg_color="#00cc66",
            hover_color="#00994d"
        )
        whatsapp_button.pack(pady=10)

        facebook_button = ctk.CTkButton(
            contact_frame,
            text="Facebook",
            command=lambda: webbrowser.open("https://www.facebook.com/naeemasghar.store"),
            font=("Roboto", 12),
            height=40,
            corner_radius=8,
            fg_color="#3366ff",
            hover_color="#2952cc"
        )
        facebook_button.pack(pady=10)

        made_with_love = ctk.CTkLabel(
            contact_frame,
            text="Made with ❤️ in Army Medical College, Rawalpindi",
            font=("Roboto", 10),
            text_color="#888888"
        )
        made_with_love.pack(pady=20)

    def list_videos_or_qualities(self):
        for widget in self.video_frame.winfo_children():
            widget.destroy()
        self.checkbox_vars.clear()
        self.quality_vars.clear()
        self.quality_frames.clear()

        self.show_fetch_progress()

        fetch_thread = Thread(target=self.fetch_videos_thread)
        fetch_thread.start()

    def show_fetch_progress(self):
        self.fetch_progress_frame = ctk.CTkFrame(self.video_frame, fg_color="#2d2d2d", corner_radius=15)
        self.fetch_progress_frame.pack(fill="x", pady=10, padx=20)

        self.fetch_progress_label = ctk.CTkLabel(
            self.fetch_progress_frame,
            text="Fetching videos...",
            font=("Roboto", 12),
            text_color="#ffffff"
        )
        self.fetch_progress_label.pack(pady=5)

        self.fetch_progress_bar = ctk.CTkProgressBar(
            self.fetch_progress_frame,
            width=400,
            height=15,
            corner_radius=10,
            fg_color="#363636",
            progress_color="#ff4444"
        )
        self.fetch_progress_bar.pack(pady=5)
        self.fetch_progress_bar.set(0)

    def fetch_videos_thread(self):
        start_time = time.time()
        self.videos = self.get_playlist_videos()
        fetch_time = time.time() - start_time

        # Ensure the progress bar runs for at least 3 seconds
        total_time = max(3, fetch_time)
        steps = 50  # Number of steps for smooth animation
        for i in range(steps + 1):
            progress = i / steps
            self.fetch_progress_bar.set(progress)
            self.root.update_idletasks()
            time.sleep(total_time / steps)

        self.root.after(0, self.display_fetched_videos)

    def display_fetched_videos(self):
        self.fetch_progress_frame.destroy()
        
        if not self.videos:
            messagebox.showinfo("No Videos", "No videos found for the given URL.")
            return

        if len(self.videos) == 1:
            self.display_video_qualities(self.videos[0][0], self.videos[0][1])
        else:
            self.display_videos()

    def get_playlist_videos(self) -> List[Tuple[str, str]]:
        url = self.url_entry.get()
        ydl_opts = {
            'quiet': True,
            'extract_flat': 'in_playlist',
            'force_generic_extractor': False,
            'ignoreerrors': True,
            'extractor_args': {
                'youtube': {
                    'player_client': ['android', 'web']
                }
            },
        }
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                result = ydl.extract_info(url, download=False)
                if 'entries' in result:
                    # This is a playlist or channel
                    videos = []
                    for entry in result['entries']:
                        if isinstance(entry, dict) and entry.get('title') and entry.get('url'):
                            videos.append((entry['title'], entry['url']))
                        elif isinstance(entry, dict) and 'entries' in entry:
                            # This is likely a channel with multiple pages
                            for video in entry['entries']:
                                if video.get('title') and video.get('url'):
                                    videos.append((video['title'], video['url']))
                    return videos
                else:
                    # This is a single video
                    return [(result['title'], result['webpage_url'])]
        except Exception as e:
            messagebox.showerror("Error", f"Error retrieving video/playlist/channel info: {e}")
            return []

    def start_download(self):
        if len(self.videos) == 1:
            selected_videos = [0]
        else:
            selected_videos = [i for i, var in enumerate(self.checkbox_vars) if var.get()]
        
        if not selected_videos:
            messagebox.showwarning("Selection Error", "Please select at least one video to download.")
            return

        self.create_progress_window()
        download_thread = Thread(target=self.download_videos, args=(selected_videos,))
        download_thread.start()

    def download_videos(self, selected_videos: List[int]):
        if os.name == 'nt':
            download_dir = r'C:\Youtube Videos'
        else:
            download_dir = os.path.join(os.path.expanduser('~'), 'Downloads', 'Youtube Videos')
        os.makedirs(download_dir, exist_ok=True)

        for index in selected_videos:
            video_title, video_url = self.videos[index]
            self.file_name_label.configure(text=f"Downloading: {video_title}")

            selected_quality = (self.quality_vars.get(index, ctk.StringVar(value='720p')).get() 
                              if index in self.quality_vars 
                              else getattr(self, 'selected_quality', ctk.StringVar(value='720p')).get())

            ydl_opts = {
                'format': f'bestvideo[height<={selected_quality[:-1]}]+bestaudio/best[height<={selected_quality[:-1]}]/bestvideo+bestaudio/best',
                'outtmpl': os.path.join(download_dir, '%(title)s.%(ext)s'),
                'ignoreerrors': True,
                'socket_timeout': 300,
                'merge_output_format': 'mp4',
                'progress_hooks': [self.progress_hook],
                'no_warnings': True,
                'quiet': True,
                'extractor_args': {
                    'youtube': {
                        'player_client': ['android', 'web']
                    }
                },
            }

            try:
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    ydl.download([video_url])
            except Exception as e:
                self.file_name_label.configure(text=f"Error downloading: {video_title}")
                error_message = f"Failed to download {video_title}: {str(e)}\n\nError details: {type(e).__name__}"
                print(error_message)
                messagebox.showerror("Download Error", error_message)
                continue

            self.progress_bar.set(0)
            self.size_label.configure(text="0 MB / 0 MB")
            self.speed_label.configure(text="0 KB/s")
            self.eta_label.configure(text="Time remaining: --:--")

        self.file_name_label.configure(text="All downloads completed!")
        self.progress_window.after(2000, self.progress_window.destroy)

    def progress_hook(self, d):
        if d['status'] == 'downloading':
            try:
                downloaded = d.get('downloaded_bytes', 0)
                total = d.get('total_bytes') or d.get('total_bytes_estimate', 0)
                
                if total > 0:
                    self.progress_bar.set(downloaded / total)

                downloaded_mb = downloaded / (1024 * 1024)
                total_mb = total / (1024 * 1024) if total else 0
                speed = d.get('speed', 0) / 1024 if d.get('speed') else 0
                eta = d.get('eta', 0)

                self.size_label.configure(text=f"{downloaded_mb:.1f} MB / {total_mb:.1f} MB")

                speed_text = f"{speed:.1f} KB/s" if speed < 1024 else f"{speed/1024:.1f} MB/s"
                self.speed_label.configure(text=speed_text)

                if eta is not None:
                    minutes, seconds = divmod(eta, 60)
                    self.eta_label.configure(text=f"Time remaining: {minutes:02d}:{seconds:02d}")
                else:
                    self.eta_label.configure(text="Time remaining: Unknown")
            except Exception as e:
                print(f"Error in progress_hook: {str(e)}")

        elif d['status'] == 'finished':
            self.file_name_label.configure(text=f"Finished downloading: {d['filename']}")
            self.progress_bar.set(1)
            self.speed_label.configure(text="")
            self.eta_label.configure(text="Time remaining: 00:00")

if __name__ == "__main__":
    YouTubeVideoDownloader()

