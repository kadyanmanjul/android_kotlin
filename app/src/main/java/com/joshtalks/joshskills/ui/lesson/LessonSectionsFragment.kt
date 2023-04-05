package com.joshtalks.joshskills.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.databinding.FragmentLessonSectionsBinding
import com.joshtalks.joshskills.R

class LessonSectionsFragment : Fragment(), LessonSectionsListAdapter.OnSectionClickLister {

    private lateinit var binding: FragmentLessonSectionsBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_lesson_sections,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = ArrayList<SectionModel>()
        list.add(SectionModel("Video Lesson", "15 minutes"))
        list.add(SectionModel("Speaking Lesson", "45 minutes"))
        list.add(SectionModel("Vocab Lesson", "25 minutes"))
        list.add(SectionModel("Reading Lesson", "35 minutes"))


        binding.rvListOfEctions.adapter = LessonSectionsListAdapter(list, this)
        binding.rvListOfEctions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListOfEctions.hasFixedSize()

        binding.txtActivitiesCount.text = "${list.size} Activities"
    }

    companion object {
        fun newInstance() =
            LessonSectionsFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onSectionClick(sectionName:String) {
        when(sectionName){
            "Video Lesson" -> {
                findNavController().navigate(LessonSectionsFragmentDirections.actionLessonSectionsFragmentToGrammarFragment())
            }
            "Speaking Lesson" ->{
                findNavController().navigate(LessonSectionsFragmentDirections.actionLessonSectionsFragmentToSpeakingPractiseFragment())
            }
            "Vocab Lesson" ->{
                findNavController().navigate(LessonSectionsFragmentDirections.actionLessonSectionsFragmentToVocabularyFragment())
            }
            "Reading Lesson" ->{
                findNavController().navigate(LessonSectionsFragmentDirections.actionLessonSectionsFragmentToReadingFragmentWithoutFeedback())
            }
        }
    }
}