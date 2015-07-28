module RakeUtils
    # Change the extension of a file
    def replace_ext file, new_ext
        in_ext = File.extname file
        "#{File.dirname file}/#{File.basename file, in_ext}.#{new_ext}"
    end

    # Replace a prepended directory on a file
    def replace_dir src_dir, dst_dir, file
        raise "no matches!" unless m = /^#{src_dir}\/(.*)$/.match(file)
        "#{dst_dir}/#{m[1]}"
    end

    # Compose the above
    def replace_dir_ext src_dir, dst_dir, new_ext, file
        replace_dir(src_dir, dst_dir, replace_ext(file, new_ext))
    end

    def get_src_dst src_dir, src_ext, dst_dir, dst_ext
        src_files = Rake::FileList.new("#{src_dir}/**/*.#{src_ext}")

        dst_files = src_files.map do |f|
            replace_dir_ext( src_dir,dst_dir, dst_ext, f)
        end

        [src_files, dst_files]
    end

end
