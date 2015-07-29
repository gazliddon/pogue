module RakeUtils

    # removing any prepending dots from an extension
    def remove_dot ext
        ext.slice( 1, ext.length - 1 ) if ext[0] == '.'
    end

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
        dst_files = src_files.pathmap("%{^#{src_dir}/,#{dst_dir}/}X." + dst_ext)

        [src_files, dst_files]
    end

    # get the src file from the dst file name
    def get_src_file src_dir, src_ext, dst_dir, f
        replace_dir_ext(dst_dir, src_dir, src_ext, f)
    end

    def grule( hash )
        dst_spec = hash.keys[0]
        src_dir, src_ext = hash[dst_spec]
        dst_dir, dst_ext = dst_spec

        dst_ext = remove_dot dst_ext
        src_ext = remove_dot src_ext

        rule( /^#{dst_dir}\/.*\.#{dst_ext}/ =>
             [proc {|task_name| 
                 get_src_file(src_dir, src_ext, dst_dir, task_name)}] ) do |t|
                     yield t
                 end
    end

end